package com.example.backend.course.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class RecommendedTagControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `추천 태그 모킹 응답을 내려준다`() {
            mockMvc
                .perform(get("/api/v1/recommended-tags"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.tags[0]").value("감성카페"))
        }

        @Test
        fun `limit으로 개수를 제한한다`() {
            mockMvc
                .perform(get("/api/v1/recommended-tags?limit=2"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.tags.length()").value(2))
        }

        @Test
        fun `limit이 범위를 벗어나면 4001 에러`() {
            mockMvc
                .perform(get("/api/v1/recommended-tags?limit=0"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `mockError 파라미터를 주입하면 모킹 에러가 내려간다`() {
            mockMvc
                .perform(get("/api/v1/recommended-tags?mockError=4001"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
                .andExpect(jsonPath("$.data").doesNotExist())
        }
    }
