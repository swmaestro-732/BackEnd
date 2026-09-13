package com.example.backend.course.adapter.inbound.web.request

import com.example.backend.course.domain.model.CourseVisibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ForkCourseRequestTest {
    @Test
    fun `toCommand — userId 와 forkedFromId 가 command 에 그대로 전달된다`() {
        val request =
            ForkCourseRequest(
                title = "포크 코스",
                description = "설명",
                thumbnailUrl = "https://cdn.example.com/cover.jpg",
                tags = listOf("FOOD", "CAFETOUR"),
                visibility = CourseVisibility.PUBLIC,
                isPublished = true,
                places = emptyList(),
            )

        val cmd = request.toCommand(userId = 10L, forkedFromId = 99L)

        assertEquals(10L, cmd.userId)
        assertEquals(99L, cmd.forkedFromId)
    }

    @Test
    fun `toCommand — 제목·설명·커버·공개설정이 command 로 매핑된다`() {
        val request =
            ForkCourseRequest(
                title = "나만의 코스",
                description = "내 설명",
                thumbnailUrl = "https://cdn.example.com/thumb.jpg",
                tags = emptyList(),
                visibility = CourseVisibility.FOLLOWER,
                isPublished = false,
                places = emptyList(),
            )

        val cmd = request.toCommand(userId = 1L, forkedFromId = 50L)

        assertEquals("나만의 코스", cmd.title)
        assertEquals("내 설명", cmd.description)
        assertEquals("https://cdn.example.com/thumb.jpg", cmd.coverImageUrl)
        assertEquals(CourseVisibility.FOLLOWER, cmd.visibility)
        assertEquals(false, cmd.isPublished)
    }

    @Test
    fun `toCommand — nullable 필드(description, thumbnailUrl)가 null 이면 command 에도 null 이다`() {
        val request =
            ForkCourseRequest(
                title = "",
                description = null,
                thumbnailUrl = null,
                tags = emptyList(),
                visibility = CourseVisibility.PRIVATE,
                isPublished = false,
                places = emptyList(),
            )

        val cmd = request.toCommand(userId = 1L, forkedFromId = 2L)

        assertNull(cmd.description)
        assertNull(cmd.coverImageUrl)
    }

    @Test
    fun `toCommand — tags 가 command 에 그대로 전달된다`() {
        val tags = listOf("FOOD", "CULTURE", "SHOPPING")
        val request =
            ForkCourseRequest(
                title = "태그 테스트",
                description = null,
                thumbnailUrl = null,
                tags = tags,
                visibility = CourseVisibility.PUBLIC,
                isPublished = false,
                places = emptyList(),
            )

        val cmd = request.toCommand(userId = 1L, forkedFromId = 2L)

        assertEquals(tags, cmd.tags)
    }

    @Test
    fun `toCommand — places 가 CreateCoursePlaceCommand 로 변환된다`() {
        val placeRequest =
            CreateCoursePlaceRequest(
                placeId = 7L,
                orderNo = 0,
                caption = "첫 번째 장소",
                imageUrls = listOf("https://cdn.example.com/img.jpg"),
                walkingMinutes = 10,
            )
        val request =
            ForkCourseRequest(
                title = "장소 포함 코스",
                description = null,
                thumbnailUrl = null,
                tags = emptyList(),
                visibility = CourseVisibility.PUBLIC,
                isPublished = false,
                places = listOf(placeRequest),
            )

        val cmd = request.toCommand(userId = 1L, forkedFromId = 5L)

        assertEquals(1, cmd.places.size)
        val place = cmd.places.first()
        assertEquals(7L, place.placeId)
        assertEquals(0, place.orderNo)
        assertEquals("첫 번째 장소", place.caption)
        assertEquals(listOf("https://cdn.example.com/img.jpg"), place.imageUrls)
        assertEquals(10, place.walkingMinutes)
    }

    @Test
    fun `toCommand — places 가 비어 있으면 command 의 places 도 비어 있다`() {
        val request =
            ForkCourseRequest(
                title = "",
                description = null,
                thumbnailUrl = null,
                tags = emptyList(),
                visibility = CourseVisibility.PUBLIC,
                isPublished = false,
                places = emptyList(),
            )

        val cmd = request.toCommand(userId = 1L, forkedFromId = 2L)

        assertTrue(cmd.places.isEmpty())
    }
}
