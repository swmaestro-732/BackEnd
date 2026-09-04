package com.example.backend.mobile.course.application.service

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.course.application.port.inbound.dto.CourseDetailResult
import com.example.backend.course.application.port.inbound.dto.CoursePlaceResult
import com.example.backend.course.domain.model.CourseVisibility
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.user.application.port.inbound.UserUseCase
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class CourseMobileServiceTest {
    private val courseQueryUseCase = mock(CourseQueryUseCase::class.java)
    private val userUseCase = mock(UserUseCase::class.java)
    private val placeQueryUseCase = mock(PlaceQueryUseCase::class.java)
    private val service = CourseMobileService(courseQueryUseCase, userUseCase, placeQueryUseCase)

    @Test
    fun `코스 작성자와 장소를 조회해 화면 결과로 조립한다`() {
        val course = course(placeIds = listOf(30L, 20L))
        val author = mock(UserProfileResult::class.java)
        val places = listOf(mock(PlaceSummary::class.java), mock(PlaceSummary::class.java))
        `when`(courseQueryUseCase.getDetail(10L, 7L)).thenReturn(course)
        `when`(userUseCase.getProfile(3L, 7L)).thenReturn(author)
        `when`(placeQueryUseCase.findPlacesById(listOf(30L, 20L))).thenReturn(places)

        val result = service.코스상세화면조회(10L, 7L)

        assertSame(course, result.course)
        assertSame(author, result.author)
        assertSame(places, result.places)
        verify(courseQueryUseCase).getDetail(10L, 7L)
        verify(userUseCase).getProfile(3L, 7L)
        verify(placeQueryUseCase).findPlacesById(listOf(30L, 20L))
    }

    private fun course(placeIds: List<Long>) =
        CourseDetailResult(
            id = 10L,
            title = "코스",
            coverImageUrl = "cover",
            theme = "CAFETOUR",
            area = "성수동1가",
            tags = emptyList(),
            description = "설명",
            visibility = CourseVisibility.PUBLIC,
            authorId = 3L,
            tracingsCnt = 0,
            places =
                placeIds.mapIndexed { index, id ->
                    CoursePlaceResult(index.toLong(), id, index, null, null, emptyList())
                },
            hasSaved = false,
            hasStartedCourse = false,
        )
}
