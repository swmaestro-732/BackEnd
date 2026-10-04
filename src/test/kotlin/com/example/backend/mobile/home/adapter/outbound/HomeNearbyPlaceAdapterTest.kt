package com.example.backend.mobile.home.adapter.outbound

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.NearbyPlaceSummary
import com.example.backend.user.application.port.inbound.SavedPlaceUseCase
import com.example.backend.user.application.port.inbound.dto.SavedPlaceRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`

/**
 * [HomeNearbyPlaceAdapter] 단위 테스트 — 저장 목록(user)과 거리순 조회(place)를 잇는 규칙을 검증한다.
 * 검증 대상: 저장 0개면 엔진 미호출, 방문 여부 병합, 검색 503 만 빈 목록으로 흡수(그 외 오류는 전파).
 */
class HomeNearbyPlaceAdapterTest {
    private val savedPlaceUseCase = mock(SavedPlaceUseCase::class.java)
    private val placeQueryUseCase = mock(PlaceQueryUseCase::class.java)
    private val adapter = HomeNearbyPlaceAdapter(savedPlaceUseCase, placeQueryUseCase)

    @Test
    fun `저장 장소가 없으면 거리순 조회를 부르지 않고 빈 목록이다`() {
        `when`(savedPlaceUseCase.listSavedPlaceRefs(USER_ID)).thenReturn(emptyList())

        assertTrue(adapter.findNearbySavedPlaces(USER_ID, ORIGIN, 5).isEmpty())
        verifyNoInteractions(placeQueryUseCase)
    }

    @Test
    fun `거리순 결과에 저장 레코드의 방문 여부를 합치고 엔진 순서를 유지한다`() {
        `when`(savedPlaceUseCase.listSavedPlaceRefs(USER_ID))
            .thenReturn(listOf(SavedPlaceRef(10, visited = false), SavedPlaceRef(20, visited = true)))
        `when`(placeQueryUseCase.findNearest(listOf(10L, 20L), ORIGIN, 5))
            .thenReturn(listOf(nearby(20, 150.0), nearby(10, 820.0)))

        val result = adapter.findNearbySavedPlaces(USER_ID, ORIGIN, 5)

        assertEquals(listOf(20L, 10L), result.map { it.placeId })
        assertEquals(listOf(true, false), result.map { it.visited })
        assertEquals(150.0, result[0].distanceMeters)
        assertEquals(4.8, result[0].rating)
        assertEquals(212, result[0].ratingCount)
        assertEquals("CAFE", result[0].category)
    }

    @Test
    fun `검색엔진 장애(503)는 빈 목록으로 흡수한다`() {
        `when`(savedPlaceUseCase.listSavedPlaceRefs(USER_ID)).thenReturn(listOf(SavedPlaceRef(10, false)))
        `when`(placeQueryUseCase.findNearest(listOf(10L), ORIGIN, 5))
            .thenThrow(BusinessException(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE))

        assertTrue(adapter.findNearbySavedPlaces(USER_ID, ORIGIN, 5).isEmpty())
    }

    @Test
    fun `검색 장애가 아닌 오류는 그대로 전파한다`() {
        `when`(savedPlaceUseCase.listSavedPlaceRefs(USER_ID)).thenReturn(listOf(SavedPlaceRef(10, false)))
        `when`(placeQueryUseCase.findNearest(listOf(10L), ORIGIN, 5))
            .thenThrow(BusinessException(CommonErrorCode.INVALID_INPUT))

        val ex = assertThrows<BusinessException> { adapter.findNearbySavedPlaces(USER_ID, ORIGIN, 5) }
        assertEquals(CommonErrorCode.INVALID_INPUT, ex.errorCode)
    }

    private fun nearby(
        id: Long,
        distanceMeters: Double,
    ) = NearbyPlaceSummary(
        id = id,
        name = "장소 $id",
        category = "CAFE",
        imageUrl = null,
        rating = 4.8,
        ratingCount = 212,
        distanceMeters = distanceMeters,
    )

    private companion object {
        const val USER_ID = 7L
        val ORIGIN = Coordinate(37.544, 127.056)
    }
}
