package com.example.backend.place.adapter.inbound.web

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.place.application.port.outbound.PlaceMapHits
import com.example.backend.place.application.port.outbound.PlaceMapSearchPort
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.application.port.outbound.PlaceSearchHits
import com.example.backend.place.application.port.outbound.PlaceSearchQueryPort
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 라우팅·파라미터 검증 — 검색엔진 포트는 목으로 대체한다(CI 에 OpenSearch 가 없다). */
@AutoConfigureMockMvc
class PlaceSearchRoutesTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
    ) : IntegrationTestBase() {
        @MockitoBean
        private lateinit var searchPort: PlaceSearchQueryPort

        @MockitoBean
        private lateinit var mapPort: PlaceMapSearchPort

        // Mockito any() 는 null 을 돌려주므로 non-null 파라미터에는 더미로 대체한다(매처 등록은 유지된다).
        private fun anyCriteria(): PlaceSearchCriteria =
            any() ?: PlaceSearchCriteria(emptyList(), emptyList(), emptyList(), null, 0, 0)

        @Test
        fun `목록 검색은 뷰포트 없이 페이지 응답을 반환한다`() {
            `when`(searchPort.search(anyCriteria())).thenReturn(PlaceSearchHits(emptyList(), 0))

            mvc
                .perform(get("/api/v1/places").param("q", "존재하지않는검색테스트장소"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.places").isArray)
                .andExpect(jsonPath("$.data.hasNext").value(false))
        }

        @Test
        fun `검색엔진 미가용이면 DB 폴백 없이 503 을 반환한다`() {
            `when`(searchPort.search(anyCriteria()))
                .thenThrow(BusinessException(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE))

            mvc
                .perform(get("/api/v1/places").param("q", "카페"))
                .andExpect(status().isServiceUnavailable)
                .andExpect(jsonPath("$.code").value(PlaceErrorCode.PLACE_SEARCH_UNAVAILABLE.code))
        }

        @Test
        fun `잘못된 목록 개수와 기준 장소는 400이다`() {
            mvc
                .perform(get("/api/v1/places").param("q", "카페").param("size", "0"))
                .andExpect(status().isBadRequest)
            mvc
                .perform(get("/api/v1/places").param("q", "카페").param("anchorPlaceId", "-1"))
                .andExpect(status().isBadRequest)
        }

        @Test
        fun `지도 검색은 줌과 키워드 없이 마커 클러스터 응답을 반환한다`() {
            `when`(mapPort.searchMap(anyCriteria(), anyInt())).thenReturn(PlaceMapHits(0, emptyList(), emptyList()))

            mvc
                .perform(
                    get("/api/v1/places/map")
                        .param("swLat", "37.5")
                        .param("swLng", "127.0")
                        .param("neLat", "37.6")
                        .param("neLng", "127.1")
                        .param("category", "CAFE"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.places").isArray)
                .andExpect(jsonPath("$.data.clusters").isArray)
                .andExpect(jsonPath("$.data.hasNext").doesNotExist())
        }

        @Test
        fun `지도 검색은 뷰포트 누락과 면적이 없는 영역을 400으로 반환한다`() {
            mvc.perform(get("/api/v1/places/map")).andExpect(status().isBadRequest)
            mvc
                .perform(
                    get("/api/v1/places/map")
                        .param("swLat", "37.5")
                        .param("swLng", "127.0")
                        .param("neLat", "37.6")
                        .param("neLng", "127.0"),
                ).andExpect(status().isBadRequest)
        }
    }
