package com.example.backend.mobile.home.adapter.inbound.web.response

import com.example.backend.mobile.home.application.port.inbound.dto.HomeResult
import com.example.backend.mobile.home.application.port.outbound.dto.HomeNearbyPlace
import com.example.backend.mobile.home.application.port.outbound.dto.HomeProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** [HomeResponse.from] 단위 테스트 — 근처 저장 장소 매핑(거리 반올림, 방문 여부)과 프로필 매핑을 검증한다. */
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
}
