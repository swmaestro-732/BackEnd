package com.example.backend.course.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@Sql(scripts = ["/sql/recommended-tags-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RecommendedTagControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
    ) : IntegrationTestBase() {
        @Test
        fun `placeIds가 있으면 장소가 담긴 코스들의 태그를 앞세우고 인기 태그로 채운다`() {
            mockMvc
                .perform(get("/api/v1/recommended-tags?placeIds=100"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.tags[0]").value("감성카페"))
                .andExpect(jsonPath("$.data.tags[1]").value("통창뷰"))
                .andExpect(jsonPath("$.data.tags[2]").value("데이트"))
                .andExpect(jsonPath("$.data.tags.length()").value(3))
        }

        @Test
        fun `placeIds가 없으면 인기 태그를 내려준다`() {
            mockMvc
                .perform(get("/api/v1/recommended-tags"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.tags[0]").value("감성카페"))
                .andExpect(jsonPath("$.data.tags.length()").value(3))
        }

        @Test
        fun `limit으로 개수를 제한한다`() {
            mockMvc
                .perform(get("/api/v1/recommended-tags?placeIds=100&limit=1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.tags.length()").value(1))
                .andExpect(jsonPath("$.data.tags[0]").value("감성카페"))
        }

        @Test
        fun `limit이 범위를 벗어나면 4002와 fieldErrors`() {
            mockMvc
                .perform(get("/api/v1/recommended-tags?limit=0"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("limit"))
                .andExpect(jsonPath("$.fieldErrors[0].reason").value("1 이상이어야 합니다"))
        }
    }
