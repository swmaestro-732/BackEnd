package com.example.backend.mobile.user.application.service

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.mobile.user.application.port.inbound.SavedCourseScreenCommand
import com.example.backend.mobile.user.application.port.inbound.SavedCourseScreenUseCase
import com.example.backend.mobile.user.application.port.inbound.dto.SavedCourseScreenResult
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.user.application.port.inbound.CourseInteractionUseCase
import com.example.backend.user.application.port.inbound.SavedCourseUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import com.example.backend.user.application.port.inbound.dto.SavedCoursesCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 저장함 코스 탭 화면 조합 서비스 (BFF). 도메인 인바운드 포트들을 조합해 한 화면 응답 재료를 만든다 —
 * 저장 레코드·폴더·완주 여부([SavedCourseUseCase]·[CourseInteractionUseCase]) → 코스 상세([CourseQueryUseCase])
 * → 작성자 프로필([UserUseCase]) → 코스 장소 좌표([PlaceQueryUseCase]).
 *
 * 코스 상세는 [CourseQueryUseCase.getDetails] 로 페이지 전체를 배치 조회한다(코스별 N+1 회피) — 삭제·비공개로
 * 볼 수 없게 된 코스는 결과에서 빠지므로 여기서 예외를 잡을 필요가 없다. 조합 한 번을 하나의 읽기 트랜잭션으로
 * 묶어 일관된 스냅샷을 본다.
 */
@Service
@Transactional(readOnly = true)
class SavedCourseScreenService(
    private val savedCourseUseCase: SavedCourseUseCase,
    private val courseQueryUseCase: CourseQueryUseCase,
    private val userUseCase: UserUseCase,
    private val placeQueryUseCase: PlaceQueryUseCase,
    private val courseInteractionUseCase: CourseInteractionUseCase,
) : SavedCourseScreenUseCase {
    override fun getScreen(command: SavedCourseScreenCommand): SavedCourseScreenResult {
        val saved =
            savedCourseUseCase.getSavedCourses(
                SavedCoursesCommand(
                    userId = command.userId,
                    folderId = command.folderId,
                    completed = command.completed,
                    cursor = command.cursor,
                    size = command.size,
                ),
            )
        val folders = savedCourseUseCase.getFolders(command.userId)

        val courseById =
            courseQueryUseCase
                .getDetails(saved.savedCourses.map { it.courseId }, command.userId)
                .associateBy { it.id }
        val recordsWithCourse =
            saved.savedCourses.mapNotNull { record ->
                courseById[record.courseId]?.let { record to it }
            }

        val completedAtByCourse =
            courseInteractionUseCase
                .getViewerStates(command.userId, recordsWithCourse.map { it.second.id })
                .associateBy({ it.courseId }, { it.completedAt })
        // 작성자 프로필은 한 번에 배치 조회한다(작성자 수만큼의 N+1 회피). 소프트 삭제된 작성자는 결과에서 빠지고,
        // 그 코스는 아래 items 조립에서 제외된다(삭제·비공개 코스 제외와 같은 취급).
        val authorById =
            userUseCase
                .getProfiles(recordsWithCourse.map { it.second.authorId }, command.userId)
                .associateBy { it.id }
        val placeById =
            placeQueryUseCase
                .findPlacesById(
                    recordsWithCourse
                        .flatMap { (_, course) ->
                            course.places.map { it.placeId }
                        }.distinct(),
                ).associateBy { it.id }

        val items =
            recordsWithCourse.mapNotNull { (record, course) ->
                // 작성자 프로필을 해석하지 못한(소프트 삭제 등) 코스는 목록에서 제외한다 — 삭제·비공개 코스 제외와 같은 취급.
                val author = authorById[course.authorId] ?: return@mapNotNull null
                SavedCourseScreenResult.Item(
                    savedId = record.id,
                    folderId = record.folderId,
                    savedAt = record.savedAt,
                    completedAt = completedAtByCourse[course.id],
                    course = course,
                    author = author,
                    placeById = placeById,
                )
            }

        return SavedCourseScreenResult(
            totalCount = saved.totalCount,
            completedCount = saved.completedCount,
            folders = folders,
            nextCursor = saved.nextCursor,
            hasNext = saved.hasNext,
            viewerId = command.userId,
            items = items,
        )
    }
}
