package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** 따라가기 종료 모킹 컨트롤러 통합 테스트 — 저장 없이 고정 성공과 요청 검증만 본다. */
@AutoConfigureMockMvc
class CourseTraceControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @Test
        fun `종료 — 방문 기록과 소요 시간·이동 거리를 보내면 200 과 안내 메시지를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/1/trace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "visitedPlaces": [
                                { "placeId": 22, "visitedAt": "2026-10-10T05:00:00Z" },
                                { "placeId": 25, "visitedAt": "2026-10-10T07:48:00Z" }
                              ],
                              "durationMinutes": 192,
                              "distanceMeters": 2400
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.message").value("따라가기를 종료했습니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
        }

        @Test
        fun `종료 — 방문이 없어도(빈 배열) 200 이다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/1/trace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"visitedPlaces": [], "durationMinutes": 0, "distanceMeters": 0}"""),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
        }

        @Test
        fun `종료 — 음수 소요 시간·0 이하 placeId 는 400 과 4002 fieldErrors 다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/1/trace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "visitedPlaces": [ { "placeId": 0, "visitedAt": "2026-10-10T05:00:00Z" } ],
                              "durationMinutes": -1,
                              "distanceMeters": 2400
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(
                    jsonPath("$.fieldErrors[*].field").value(hasItems("durationMinutes", "visitedPlaces[0].placeId")),
                )
        }

        @Test
        fun `종료 — 필수 필드(durationMinutes) 누락은 400 과 4001 이다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/1/trace")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenFor(USER_ID)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"visitedPlaces": [], "distanceMeters": 0}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `종료 — 토큰 없으면 401 이다`() {
            mockMvc
                .perform(
                    post("/api/v1/courses/1/trace")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"visitedPlaces": [], "durationMinutes": 0, "distanceMeters": 0}"""),
                ).andExpect(status().isUnauthorized)
        }

        private fun tokenFor(userId: Long) = jwtTokenProvider.issueAccessToken(userId)

        private companion object {
            const val USER_ID = 1L
        }
    }
