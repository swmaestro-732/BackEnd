package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 저장 장소 컨트롤러 목 경로 통합 테스트.
 * SQL 픽스처 없이 [?mock=true] 고정 응답과 [visit] 정적 응답만 검증한다.
 */
@AutoConfigureMockMvc
class SavedPlaceControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `저장 mock=true 면 DB 없이 201 과 성공 코드를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/saved-places")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"placeId": 101}"""),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.code").value(2000))
        }

        @Test
        fun `저장 취소 mock=true 면 DB 없이 200 과 성공 코드를 내려준다`() {
            mockMvc
                .perform(
                    delete("/api/v1/saved-places/101")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
        }

        @Test
        fun `방문 처리 — 인증 없이도 200 과 성공 코드를 내려준다`() {
            mockMvc
                .perform(
                    patch("/api/v1/saved-places/101")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
        }

        @Test
        fun `조회 mock=true 면 DB 없이 200 과 savedPlaces 목록을 내려준다`() {
            mockMvc
                .perform(
                    get("/api/v1/saved-places")
                        .param("mock", "true")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}"),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.savedPlaces").isArray)
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private companion object {
            const val USER_ID = 1L
        }
    }
