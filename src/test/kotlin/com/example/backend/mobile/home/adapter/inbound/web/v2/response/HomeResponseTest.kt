package com.example.backend.mobile.home.adapter.inbound.web.v2.response

import com.example.backend.mobile.home.application.port.inbound.dto.HomeResult
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace
import com.example.backend.mobile.home.application.port.outbound.dto.HomeProfile
import com.example.backend.mobile.home.application.port.outbound.dto.HomeTracingCourse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/** [HomeResponse.from] 단위 테스트 — 근처 저장 장소 매핑(거리 반올림, 방문 여부)·따라가는 중 코스·프로필 매핑을 검증한다. */
class HomeResponseTest {
    @Test
    fun `근처 저장 장소는 거리를 미터 정수로 반올림하고 나머지 필드를 그대로 옮긴다`() {
        val place =
            HomeNearbyPlace(
                placeId = 101,
                name = "어니언 성수",
                imageUrl = "https://img/onion.jpg",
                category = "CAFE",
                rating = 4.8,
                ratingCount = 212,
                distanceMeters = 400.6,
                visited = true,
            )

        val response =
            HomeResponse.from(
                HomeResult(
                    profile = HomeProfile("칠삼이", null),
                    tracingCourse = null,
                    recommendedCourses = emptyList(),
                    nearbySavedPlaces = listOf(HomeResult.NearbySavedPlace(place, walkingMinutes = 6)),
                ),
            )

        assertEquals("칠삼이", response.profile?.nickname)
        assertTrue(response.recommendedCourses.isEmpty())
        val item = response.nearbySavedPlaces.single()
        assertEquals(
            HomeResponse.NearbySavedPlace(
                placeId = 101,
                name = "어니언 성수",
                imageUrl = "https://img/onion.jpg",
                category = "CAFE",
                rating = 4.8,
                ratingCount = 212,
                distanceMeters = 401,
                walkingMinutes = 6,
                visited = true,
            ),
            item,
        )
    }

    @Test
    fun `따라가는 중 코스는 필드를 그대로 옮기고 없으면 null 이다`() {
        val startedAt = Instant.parse("2026-11-03T05:30:00Z")
        val tracing =
            HomeTracingCourse(
                traceId = 10,
                courseId = 3,
                title = "을지로 노포 골목 투어",
                coverImageUrl = "https://img/cover.jpg",
                area = "을지로",
                placeCount = 5,
                visitedPlaceCount = 2,
                startedAt = startedAt,
            )

        val response = HomeResponse.from(HomeResult(null, tracing, emptyList(), emptyList()))

        assertEquals(
            HomeResponse.TracingCourse(
                traceId = 10,
                courseId = 3,
                title = "을지로 노포 골목 투어",
                coverImageUrl = "https://img/cover.jpg",
                area = "을지로",
                placeCount = 5,
                visitedPlaceCount = 2,
                startedAt = startedAt,
            ),
            response.tracingCourse,
        )
        assertNull(HomeResponse.from(HomeResult(null, null, emptyList(), emptyList())).tracingCourse)
    }
}
