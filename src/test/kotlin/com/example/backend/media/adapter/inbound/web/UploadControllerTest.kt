package com.example.backend.media.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class UploadControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `프리사인 요청은 토큰이 없으면 401을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/uploads/presign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"purpose":"PROFILE","contentType":"image/jpeg"}"""),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        fun `프리사인 요청은 key·uploadUrl·imageUrl을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/uploads/presign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"purpose":"PROFILE","contentType":"image/jpeg"}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.key").value(matchesPattern("profile/1/[0-9a-f-]{36}\\.jpg")))
                .andExpect(jsonPath("$.data.uploadUrl").isNotEmpty)
                .andExpect(jsonPath("$.data.imageUrl").isNotEmpty)
        }

        @Test
        fun `지원하지 않는 contentType이면 415를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/uploads/presign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"purpose":"PROFILE","contentType":"application/pdf"}"""),
                ).andExpect(status().isUnsupportedMediaType)
                .andExpect(jsonPath("$.code").value(4150))
        }

        @Test
        fun `purpose가 없으면 필드 검증 에러를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/uploads/presign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"contentType":"image/jpeg"}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }
    }
