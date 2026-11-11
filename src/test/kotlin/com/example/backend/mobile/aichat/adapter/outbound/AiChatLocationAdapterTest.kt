package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.geo.Coordinate
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/** [AiChatLocationAdapter] — 좌표 → 법정동코드 판정을 place 포트에 위임하고, 엔진 장애(503)는 null 로 흡수하는지 검증한다. */
class AiChatLocationAdapterTest {
    private val placeQueryUseCase = mock(PlaceQueryUseCase::class.java)
    private val adapter = AiChatLocationAdapter(placeQueryUseCase)
    private val here = Coordinate(37.5446, 127.0559)

    @Test
    fun `place 포트가 고른 법정동코드를 그대로 돌려준다`() {
        `when`(placeQueryUseCase.findDominantAreaCodeNear(here)).thenReturn("1120011400")

        assertThat(adapter.findAreaCodeAt(here)).isEqualTo("1120011400")
    }

    @Test
    fun `근처에 장소가 없으면 null`() {
        `when`(placeQueryUseCase.findDominantAreaCodeNear(here)).thenReturn(null)

        assertThat(adapter.findAreaCodeAt(here)).isNull()
    }

    @Test
    fun `검색 엔진 장애(503)는 null 로 흡수한다`() {
        `when`(placeQueryUseCase.findDominantAreaCodeNear(here))
            .thenThrow(BusinessException(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE))

        assertThat(adapter.findAreaCodeAt(here)).isNull()
    }
}
