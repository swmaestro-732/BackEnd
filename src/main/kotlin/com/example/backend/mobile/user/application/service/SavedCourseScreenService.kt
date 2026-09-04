package com.example.backend.mobile.user.application.service

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.mobile.user.application.port.inbound.SavedCourseScreenCommand
import com.example.backend.mobile.user.application.port.inbound.SavedCourseScreenUseCase
import com.example.backend.mobile.user.application.port.inbound.dto.SavedCourseScreenResult
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.user.application.port.inbound.CourseInteractionUseCase
import com.example.backend.user.application.port.inbound.SavedCourseUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import com.example.backend.user.application.port.inbound.dto.SavedCoursesCommand
import com.example.backend.user.application.port.inbound.dto.SavedCoursesResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.collections.map
import kotlin.collections.mapNotNull

/**
 * 저장함 코스 탭 화면 조합 서비스 (BFF).
 */
@Service
@Transactional(readOnly = true)
class SavedCourseScreenService(
    private val savedCourseUseCase: SavedCourseUseCase,
    private val courseQueryUseCase: CourseQueryUseCase,
    private val userUseCase: UserUseCase,
    private val placeQueryUseCase: PlaceQueryUseCase,
    private val courseInteractionUseCase: CourseInteractionUseCase,
    queryUseCase: CourseQueryUseCase,
) : SavedCourseScreenUseCase {
    override fun 저장함코스화면조회(command: SavedCourseScreenCommand): SavedCourseScreenResult {
        val saved = savedCourseUseCase.getSavedCourses(command.저장코스조회조건())
        val folderCounts = savedCourseUseCase.getFolderCounts(command.userId)
        val items = 화면항목조립(saved.savedCourses, command.userId)

        return SavedCourseScreenResult(
            totalCount = saved.totalCount,
            completedCount = saved.completedCount,
            folders = folderCounts.folders,
            withoutFolderCount = folderCounts.withoutFolderCount,
            nextCursor = saved.nextCursor,
            hasNext = saved.hasNext,
            viewerId = command.userId,
            items = items,
        )
    }

    private fun 코스상세연결(
        records: List<SavedCoursesResult.SavedCourseItem>,
        viewerId: Long,
    ): List<Pair<SavedCoursesResult.SavedCourseItem, CourseDetailResult>> {
        val courseById =
            courseQueryUseCase
                .getDetails(records.map { it.courseId }, viewerId)
                .associateBy { it.id }
        return records.mapNotNull { record -> courseById[record.courseId]?.let { record to it } }
    }

    private fun 화면항목조립(
        records: List<SavedCoursesResult.SavedCourseItem>,
        viewerId: Long,
    ): List<SavedCourseScreenResult.Item> {
        val recordsWithCourse = 코스상세연결(records, viewerId)
        val courses = recordsWithCourse.map { it.second }

        val completedAtByCourse =
            courseInteractionUseCase
                .getViewerStates(viewerId, courses.map { it.id })
                .associateBy({ it.courseId }, { it.completedAt })
        val authorById =
            userUseCase.getProfiles(courses.map { it.authorId }, viewerId).associateBy { it.id }
        val placeById =
            placeQueryUseCase
                .findPlacesById(courses.flatMap { c -> c.places.map { it.placeId } }.distinct())
                .associateBy { it.id }

        return recordsWithCourse.mapNotNull { (record, course) ->
            // 작성자 프로필을 해석하지 못한(소프트 삭제 등) 코스는 제외 — 삭제·비공개 코스 제외와 같은 취급.
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
    }

    private fun SavedCourseScreenCommand.저장코스조회조건() =
        SavedCoursesCommand(
            userId = userId,
            folderId = folderId,
            completed = completed,
            cursor = cursor,
            size = size,
        )
}
