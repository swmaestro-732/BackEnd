package com.example.backend.place.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class PlaceSearchRoutesTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `목록 검색은 뷰포트 없이 페이지 응답을 반환한다`() {
            mvc
                .perform(get("/api/v1/places").param("q", "존재하지않는검색테스트장소"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.places").isArray)
                .andExpect(jsonPath("$.data.hasNext").value(false))
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
