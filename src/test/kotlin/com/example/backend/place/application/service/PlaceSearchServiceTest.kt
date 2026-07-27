package com.example.backend.place.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.KakaoPlaceSearchPort
import com.example.backend.place.application.port.outbound.NaverPlaceSearchPort
import com.example.backend.place.domain.model.ExternalPlace
import com.example.backend.place.domain.model.ExternalPlaceSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaceSearchServiceTest {
    private fun place(
        name: String,
        source: ExternalPlaceSource,
        lat: Double,
        lng: Double,
        roadAddress: String? = null,
    ) = ExternalPlace(
        name = name,
        category = "카페",
        roadAddress = roadAddress,
        address = null,
        coordinate = Coordinate(latitude = lat, longitude = lng),
        telephone = null,
        source = source,
    )

    private class FakeNaver(
        val results: List<ExternalPlace>,
    ) : NaverPlaceSearchPort {
        override fun search(
            query: String,
            near: Coordinate?,
        ): List<ExternalPlace> = results
    }

    private class FakeKakao(
        val results: List<ExternalPlace>,
    ) : KakaoPlaceSearchPort {
        override fun search(
            query: String,
            near: Coordinate?,
        ): List<ExternalPlace> = results
    }

    @Test
    fun `네이버를 앞세우고 카카오로 채우되 겹치는 장소는 제거한다`() {
        val naver =
            listOf(
                place("어니언 성수", ExternalPlaceSource.NAVER, 37.5445, 127.0578),
            )
        val kakao =
            listOf(
                // 정규화 이름 동일 + 좌표 50m 이내 → 네이버 것과 중복 → 제거.
                place("어니언  성수", ExternalPlaceSource.KAKAO, 37.54451, 127.05781),
                // 별개 장소 → 유지.
                place("센터커피 성수", ExternalPlaceSource.KAKAO, 37.5463, 127.0537),
            )

        val result = PlaceSearchService(FakeNaver(naver), FakeKakao(kakao)).search("성수 카페", null)

        assertEquals(2, result.size)
        assertEquals("어니언 성수", result[0].name)
        assertEquals(ExternalPlaceSource.NAVER, result[0].source)
        assertEquals("센터커피 성수", result[1].name)
        assertEquals(ExternalPlaceSource.KAKAO, result[1].source)
    }

    @Test
    fun `이름이 달라도 도로명 주소가 같으면 중복으로 제거한다`() {
        val naver =
            listOf(
                place("대림창고", ExternalPlaceSource.NAVER, 37.5418, 127.0592, roadAddress = "서울 성동구 성수이로 78"),
            )
        val kakao =
            listOf(
                place("대림창고 갤러리", ExternalPlaceSource.KAKAO, 37.5500, 127.0700, roadAddress = "서울 성동구 성수이로 78"),
            )

        val result = PlaceSearchService(FakeNaver(naver), FakeKakao(kakao)).search("대림창고", null)

        assertEquals(1, result.size)
        assertEquals(ExternalPlaceSource.NAVER, result[0].source)
    }
}
