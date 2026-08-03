package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.course.application.port.inbound.CourseCounterUseCase
import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.user.application.port.inbound.SavedCourseUseCase
import com.example.backend.user.application.port.inbound.dto.SavedCourseFolderCount
import com.example.backend.user.application.port.inbound.dto.SavedCoursesCommand
import com.example.backend.user.application.port.inbound.dto.SavedCoursesResult
import com.example.backend.user.application.port.outbound.SavedCoursePersistencePort
import com.example.backend.user.domain.model.SavedCourse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 코스 저장 유스케이스 — 저장과 저장 코스 조회를 함께 담당한다.
 *
 * 저장(save): 저장할 코스가 실제로 존재하는지 코스 인바운드 포트로 검증하고(그 외 404),
 *   folderId 가 있으면 소유권을 검증하고(그 외 400), 이미 저장한 코스면 중복 저장으로 막는다(409).
 *   — 코스당 저장 레코드는 1개다. course_id 는 cross-domain(FK 없음)이라 존재 검증은 [CourseQueryUseCase] 로 한다.
 * 조회(getSavedCourses): 저장 레코드를 최신 저장순(id 내림차순)으로 커서 페이지네이션해 반환한다.
 *
 * 기본은 읽기 전용 트랜잭션이고, 쓰기(save)만 메서드 레벨에서 재정의한다.
 */
@Service
@Transactional(readOnly = true)
class SavedCourseService(
    private val savedCoursePersistencePort: SavedCoursePersistencePort,
    private val courseQueryUseCase: CourseQueryUseCase,
    private val courseCounterUseCase: CourseCounterUseCase,
) : SavedCourseUseCase {
    @Transactional
    override fun save(
        userId: Long,
        courseId: Long,
        folderId: Long?,
    ): SavedCourse {
        if (!courseQueryUseCase.existsById(courseId)) {
            throw BusinessException(ErrorCode.COURSE_NOT_FOUND, "저장할 코스를 찾을 수 없습니다: courseId=$courseId")
        }

        if (folderId != null && !savedCoursePersistencePort.existsFolder(userId, folderId)) {
            throw BusinessException(ErrorCode.INVALID_INPUT, "저장할 폴더를 찾을 수 없습니다: id=$folderId")
        }

        // 이미 저장한 코스면 폴더를 바꾸지 않고 중복 저장으로 막는다(409). 폴더 이동은 별도 API 소관.
        if (savedCoursePersistencePort.existsSavedCourse(userId, courseId)) {
            throw BusinessException(ErrorCode.COURSE_ALREADY_SAVED, "이미 저장한 코스입니다: courseId=$courseId")
        }

        val saved = savedCoursePersistencePort.insert(userId, courseId, folderId)
        // courses.saves_cnt 낙관적 +1 — 코스 도메인이 자기 카운터를 소유한다. 같은 트랜잭션에 묶이며 추후 비동기 집계로 옮긴다.
        courseCounterUseCase.increaseSavesCount(courseId)
        return saved
    }

    @Transactional
    override fun unsave(
        userId: Long,
        courseId: Long,
    ) {
        // 실제로 살아있던 저장을 지웠을 때만 카운터를 내린다 — 멱등 취소(이미 없음)는 no-op 라 언더플로를 막는다.
        if (savedCoursePersistencePort.deleteByUserAndCourse(userId, courseId)) {
            courseCounterUseCase.decreaseSavesCount(courseId)
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

    override fun getFolders(userId: Long): List<SavedCourseFolderCount> =
        savedCoursePersistencePort.listFolders(userId).map {
            SavedCourseFolderCount(id = it.id, name = it.name, count = it.count)
        }

    /** 커서(=저장 레코드 id)를 파싱한다. 형식이 잘못되면 400. */
    private fun decodeCursor(cursor: String): Long =
        cursor.toLongOrNull()
            ?: throw BusinessException(ErrorCode.INVALID_INPUT, "잘못된 커서입니다: $cursor")
}
