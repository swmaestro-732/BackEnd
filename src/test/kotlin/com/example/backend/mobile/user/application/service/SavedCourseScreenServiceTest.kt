package com.example.backend.mobile.user.application.service

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.domain.model.CourseVisibility
import com.example.backend.mobile.user.application.port.inbound.SavedCourseScreenCommand
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.user.application.port.inbound.CourseInteractionUseCase
import com.example.backend.user.application.port.inbound.SavedCourseUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import com.example.backend.user.application.port.inbound.dto.CourseViewerState
import com.example.backend.user.application.port.inbound.dto.SavedCourseFolderCount
import com.example.backend.user.application.port.inbound.dto.SavedCourseFolderCounts
import com.example.backend.user.application.port.inbound.dto.SavedCoursesCommand
import com.example.backend.user.application.port.inbound.dto.SavedCoursesResult
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant

class SavedCourseScreenServiceTest {
    private val savedCourseUseCase = mock(SavedCourseUseCase::class.java)
    private val courseQueryUseCase = mock(CourseQueryUseCase::class.java)
    private val userUseCase = mock(UserUseCase::class.java)
    private val placeQueryUseCase = mock(PlaceQueryUseCase::class.java)
    private val interactionUseCase = mock(CourseInteractionUseCase::class.java)
    private val service =
        SavedCourseScreenService(
            savedCourseUseCase,
            courseQueryUseCase,
            userUseCase,
            placeQueryUseCase,
            interactionUseCase,
        )

    @Test
    fun `저장 레코드에 코스 작성자 장소 완주 상태를 조립한다`() {
        val command = command()
        val savedAt = Instant.parse("2026-09-01T00:00:00Z")
        val completedAt = Instant.parse("2026-09-02T00:00:00Z")
        val record = SavedCoursesResult.SavedCourseItem(1L, 4L, 10L, savedAt)
        val saved = SavedCoursesResult(3, 1, "next", true, listOf(record))
        val folders = SavedCourseFolderCounts(listOf(SavedCourseFolderCount(4L, "데이트", 1)), 2)
        val course = course(10L, 3L, listOf(30L))
        val author = profile(3L)
        val place = place(30L)
        `when`(savedCourseUseCase.getSavedCourses(SavedCoursesCommand(7L, 4L, true, "cursor", 20))).thenReturn(saved)
        `when`(savedCourseUseCase.getFolderCounts(7L)).thenReturn(folders)
        `when`(courseQueryUseCase.getDetails(listOf(10L), 7L)).thenReturn(listOf(course))
        `when`(
            interactionUseCase.getViewerStates(7L, listOf(10L)),
        ).thenReturn(listOf(CourseViewerState(10L, true, completedAt)))
        `when`(userUseCase.getProfiles(listOf(3L), 7L)).thenReturn(listOf(author))
        `when`(placeQueryUseCase.findPlacesById(listOf(30L))).thenReturn(listOf(place))

        val result = service.getScreen(command)

        assertEquals(3, result.totalCount)
        assertEquals(1, result.completedCount)
        assertEquals("next", result.nextCursor)
        assertEquals(2, result.withoutFolderCount)
        assertEquals(completedAt, result.items.single().completedAt)
        assertSame(course, result.items.single().course)
        assertSame(author, result.items.single().author)
        assertSame(
            place,
            result.items
                .single()
                .placeById
                .getValue(30L),
        )
        verify(savedCourseUseCase).getSavedCourses(SavedCoursesCommand(7L, 4L, true, "cursor", 20))
    }

    @Test
    fun `조회할 수 없는 코스와 작성자 없는 코스는 항목에서 제외한다`() {
        val records =
            listOf(
                SavedCoursesResult.SavedCourseItem(1L, null, 10L, Instant.EPOCH),
                SavedCoursesResult.SavedCourseItem(2L, null, 20L, Instant.EPOCH),
            )
        `when`(savedCourseUseCase.getSavedCourses(SavedCoursesCommand(7L, null, null, null, 10)))
            .thenReturn(SavedCoursesResult(2, 0, null, false, records))
        `when`(savedCourseUseCase.getFolderCounts(7L)).thenReturn(SavedCourseFolderCounts(emptyList(), 2))
        `when`(courseQueryUseCase.getDetails(listOf(10L, 20L), 7L)).thenReturn(listOf(course(10L, 3L, emptyList())))
        `when`(interactionUseCase.getViewerStates(7L, listOf(10L))).thenReturn(emptyList())
        `when`(userUseCase.getProfiles(listOf(3L), 7L)).thenReturn(emptyList())
        `when`(placeQueryUseCase.findPlacesById(emptyList())).thenReturn(emptyList())

        val result = service.getScreen(command(folderId = null, completed = null, cursor = null, size = 10))

        assertEquals(2, result.totalCount)
        assertEquals(0, result.items.size)
    }

    private fun command(
        folderId: Long? = 4L,
        completed: Boolean? = true,
        cursor: String? = "cursor",
        size: Int = 20,
    ) = SavedCourseScreenCommand(7L, folderId, completed, cursor, size)

    private fun course(
        id: Long,
        authorId: Long,
        placeIds: List<Long>,
    ) = CourseDetailResult(
        id,
        "코스",
        "cover",
        "CAFETOUR",
        "성수",
        emptyList(),
        "설명",
        CourseVisibility.PUBLIC,
        authorId,
        0,
        placeIds.mapIndexed {
            index,
            placeId,
            ->
            CoursePlaceResult(index.toLong(), placeId, index, null, null, emptyList())
        },
        false,
        false,
    )

    private fun profile(id: Long) = UserProfileResult(id, "작성자", null, null, null, false, false, 0, 0, 0, 0, 0)

    private fun place(id: Long) = PlaceSummary(id, "장소", "CAFE", null, 37.5, 127.0, "주소", null)
}
