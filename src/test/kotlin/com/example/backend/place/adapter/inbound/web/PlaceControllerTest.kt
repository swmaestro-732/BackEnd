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
class PlaceControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `장소 상세 모킹 응답은 명세 스펙 필드를 내려준다`() {
            mockMvc
                .perform(get("/api/v1/places/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.place.id").value(1))
                .andExpect(jsonPath("$.data.place.name").value("어니언 성수"))
                .andExpect(jsonPath("$.data.place.categories[0]").value("카페"))
                .andExpect(jsonPath("$.data.place.location.latitude").value(37.5446))
                .andExpect(jsonPath("$.data.place.openStatus").value("OPEN"))
                .andExpect(jsonPath("$.data.place.reviewSummary.totalCount").value(128))
                .andExpect(jsonPath("$.data.place.reviewSummary.reviews.length()").value(2))
                .andExpect(jsonPath("$.data.place.reviewSummary.reviews[0].author.id").value(10))
                .andExpect(jsonPath("$.data.place.viewer.hasSaved").value(false))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
        }

        @Test
        fun `placeId 형식이 잘못되면 4001 에러`() {
            mockMvc
                .perform(get("/api/v1/places/abc"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        fun `mockError 파라미터를 주입하면 모킹 에러가 내려간다`() {
            mockMvc
                .perform(get("/api/v1/places/1?mockError=4040"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4040))
                .andExpect(jsonPath("$.data").doesNotExist())
        }
    }
