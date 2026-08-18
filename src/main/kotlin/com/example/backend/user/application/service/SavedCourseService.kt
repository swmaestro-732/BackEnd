package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.SavedCourseUseCase
import com.example.backend.user.application.port.inbound.dto.CourseFolderSummary
import com.example.backend.user.application.port.inbound.dto.SavedCourseFolderCount
import com.example.backend.user.application.port.inbound.dto.SavedCourseFolderCounts
import com.example.backend.user.application.port.inbound.dto.SavedCoursesCommand
import com.example.backend.user.application.port.inbound.dto.SavedCoursesResult
import com.example.backend.user.application.port.outbound.CourseAccessPort
import com.example.backend.user.application.port.outbound.SavedCoursePersistencePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.domain.model.CourseFolder
import com.example.backend.user.domain.model.SavedCourse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 저장 유스케이스 — 저장·저장 코스 조회·저장 폴더를 함께 담당한다.
 *
 * 저장(save): 저장할 코스가 실제로 존재하는지 코스 인바운드 포트로 검증하고(그 외 404),
 *   folderId 가 있으면 소유권을 검증하고(그 외 400), 이미 저장한 코스면 중복 저장으로 막는다(409).
 *   — 코스당 저장 레코드는 1개다. course_id 는 cross-domain(FK 없음)이라 존재 검증은 [CourseAccessPort] 로 한다(ACL).
 * 조회(getSavedCourses): 저장 레코드를 최신 저장순(id 내림차순)으로 커서 페이지네이션해 반환한다.
 * 폴더(createFolder·getFolders·getFolderList): 저장 폴더는 저장 레코드와 한 몸이라(폴더별 개수·소유권 검증) 같은 유스케이스가 맡는다.
 *
 * 기본은 읽기 전용 트랜잭션이고, 쓰기(save·unsave·createFolder)만 메서드 레벨에서 재정의한다.
 */
@Service
@Transactional(readOnly = true)
class SavedCourseService(
    private val savedCoursePersistencePort: SavedCoursePersistencePort,
    private val courseAccessPort: CourseAccessPort,
    private val userPersistencePort: UserPersistencePort,
) : SavedCourseUseCase {
    @Transactional
    override fun save(
        userId: Long,
        courseId: Long,
        folderId: Long?,
    ): SavedCourse {
        // 사용자 행을 FOR UPDATE 로 잠가 동시 탈퇴 정리와 직렬화한다(탈퇴 계정에 저장 유입 차단).
        if (userPersistencePort.lockActive(listOf(userId)).isEmpty()) {
            throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")
        }
        if (!courseAccessPort.existsCourse(courseId)) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "저장할 코스를 찾을 수 없습니다: courseId=$courseId")
        }

        if (folderId != null && !savedCoursePersistencePort.existsFolder(userId, folderId)) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "저장할 폴더를 찾을 수 없습니다: id=$folderId")
        }

        // 이미 저장한 코스면 폴더를 바꾸지 않고 중복 저장으로 막는다(409). 폴더 이동은 별도 API 소관.
        if (savedCoursePersistencePort.existsSavedCourse(userId, courseId)) {
            throw BusinessException(ErrorCode.COURSE_ALREADY_SAVED, "이미 저장한 코스입니다: courseId=$courseId")
        }

        // 예전에 저장했다 취소한 코스면 그때 소프트 삭제된 행을 지우고 새로 발행한다(행을 쌓지 않는다).
        val saved = savedCoursePersistencePort.insert(userId, courseId, folderId)
        // courses.saves_cnt 낙관적 +1 — 코스 도메인이 자기 카운터를 소유한다(같은 트랜잭션).
        // 0행이면 코스가 그 사이 비활성(작성자 탈퇴 등으로 soft delete)된 것 → 방금 삽입까지 롤백해 저장 행·카운터 불일치를 막는다.
        if (courseAccessPort.increaseSavesCount(courseId) == 0) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "저장할 코스를 찾을 수 없습니다: courseId=$courseId")
        }
        return saved
    }

    @Transactional
    override fun unsave(
        userId: Long,
        courseId: Long,
    ) {
        // save 와 같은 행 잠금으로 직렬화한다 — 저장 취소 삭제는 멱등이라 사용자 존재만 검증한다.
        if (userPersistencePort.lockActive(listOf(userId)).isEmpty()) {
            throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")
        }
        // 실제로 살아있던 저장을 지웠을 때만 카운터를 내린다 — 멱등 취소(이미 없음)는 no-op 라 언더플로를 막는다.
        if (savedCoursePersistencePort.deleteByUserAndCourse(userId, courseId)) {
            courseAccessPort.decreaseSavesCount(courseId)
        }
    }

    override fun getSavedCourses(command: SavedCoursesCommand): SavedCoursesResult {
        val cursorId = command.cursor?.let(::decodeCursor)

        // hasNext 판정을 위해 한 개 더 조회한 뒤, 페이지 크기만큼 잘라낸다. 완주 필터(command.completed)를 그대로 넘긴다.
        val rows =
            savedCoursePersistencePort.findPage(
                command.userId,
                command.folderId,
                command.completed,
                cursorId,
                command.size + 1,
            )
        val hasNext = rows.size > command.size
        val page = rows.take(command.size)

        return SavedCoursesResult(
            // 칩 배지는 완주 필터와 무관한 폴더 전체·완주 개수다 — count 는 완주 필터를 각각 null/true 로 고정해 센다.
            totalCount = savedCoursePersistencePort.count(command.userId, command.folderId, completed = null),
            completedCount = savedCoursePersistencePort.count(command.userId, command.folderId, completed = true),
            nextCursor = if (hasNext) page.lastOrNull()?.id?.toString() else null,
            hasNext = hasNext,
            savedCourses =
                page.map {
                    SavedCoursesResult.SavedCourseItem(
                        id = it.id,
                        folderId = it.folderId,
                        courseId = it.courseId,
                        savedAt = it.savedAt,
                    )
                },
        )
    }

    @Transactional
    override fun createFolder(
        userId: Long,
        name: String,
    ): CourseFolder {
        // 저장(save)과 같은 행 잠금으로 동시 탈퇴 정리와 직렬화한다(탈퇴 계정에 폴더 유입 차단).
        if (userPersistencePort.lockActive(listOf(userId)).isEmpty()) {
            throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")
        }
        // 같은 이름 폴더는 만들지 않는다 — 폴더 칩이 이름으로만 구분되기 때문. 동시 생성 경합은
        // (user_id, name) 유니크 인덱스가 막고 GlobalExceptionHandler 가 같은 409 로 변환한다.
        if (savedCoursePersistencePort.existsFolderName(userId, name)) {
            throw BusinessException(ErrorCode.FOLDER_NAME_ALREADY_TAKEN, "이미 같은 이름의 폴더가 있습니다: $name")
        }
        return savedCoursePersistencePort.insertFolder(userId, name)
    }

    override fun getFolders(userId: Long): List<CourseFolderSummary> =
        savedCoursePersistencePort.findFolders(userId).map {
            CourseFolderSummary(id = it.id, name = it.name)
        }

    override fun getFolderCounts(userId: Long): SavedCourseFolderCounts =
        SavedCourseFolderCounts(
            folders =
                savedCoursePersistencePort.listFolders(userId).map {
                    SavedCourseFolderCount(id = it.id, name = it.name, count = it.count)
                },
            // 폴더 미분류 저장은 어느 폴더에도 안 잡히므로 폴더별 개수의 합이 아니라 따로 센다.
            withoutFolderCount = savedCoursePersistencePort.countWithoutFolder(userId),
        )

    /** 커서(=저장 레코드 id)를 파싱한다. 형식이 잘못되면 400. */
    private fun decodeCursor(cursor: String): Long =
        cursor.toLongOrNull()
            ?: throw BusinessException(ErrorCode.INVALID_INPUT, "잘못된 커서입니다: $cursor")
}
