package com.example.backend.media.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.jayway.jsonpath.JsonPath
import org.hamcrest.Matchers.matchesPattern
import org.junit.jupiter.api.Assertions.assertEquals
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
                    post("/api/v1/uploads/presigned-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{"purpose":"PROFILE","images":[{"contentType":"image/jpeg","contentLength":1024}]}""",
                        ),
                ).andExpect(status().isUnauthorized)
        }

        @Test
        fun `프로필 이미지 하나의 프리사인 요청은 items 하나와 profile 키를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/uploads/presigned-urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{"purpose":"PROFILE","images":[{"contentType":"image/jpeg","contentLength":1024}]}""",
                        ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].key").value(matchesPattern("profile/1/[0-9a-f-]{36}\\.jpg")))
                .andExpect(jsonPath("$.data.items[0].uploadUrl").isNotEmpty)
                .andExpect(jsonPath("$.data.items[0].imageUrl").isNotEmpty)
        }

        @Test
        fun `장소 이미지 3개의 프리사인 요청은 서로 다른 place 키를 내려준다`() {
            val response =
                mockMvc
                    .perform(
                        post("/api/v1/uploads/presigned-urls")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(
                                """
                                {
                                  "purpose":"PLACE",
                                  "images":[
                                    {"contentType":"image/jpeg","contentLength":1024},
                                    {"contentType":"image/png","contentLength":2048},
                                    {"contentType":"image/webp","contentLength":4096}
                                  ]
                                }
                                """.trimIndent(),
                            ),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(2000))
                    .andExpect(jsonPath("$.data.items.length()").value(3))
                    .andExpect(jsonPath("$.data.items[0].key").value(matchesPattern("place/1/[0-9a-f-]{36}\\.jpg")))
                    .andExpect(jsonPath("$.data.items[0].uploadUrl").isNotEmpty)
                    .andExpect(jsonPath("$.data.items[0].imageUrl").isNotEmpty)
                    .andExpect(jsonPath("$.data.items[1].key").value(matchesPattern("place/1/[0-9a-f-]{36}\\.png")))
                    .andExpect(jsonPath("$.data.items[1].uploadUrl").isNotEmpty)
                    .andExpect(jsonPath("$.data.items[1].imageUrl").isNotEmpty)
                    .andExpect(jsonPath("$.data.items[2].key").value(matchesPattern("place/1/[0-9a-f-]{36}\\.webp")))
                    .andExpect(jsonPath("$.data.items[2].uploadUrl").isNotEmpty)
                    .andExpect(jsonPath("$.data.items[2].imageUrl").isNotEmpty)
                    .andReturn()

            val keys: List<String> = JsonPath.read(response.response.contentAsString, "$.data.items[*].key")
            assertEquals(3, keys.toSet().size)
        }

        @Test
        fun `코스 대표 이미지 하나의 프리사인 요청은 course 키를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/uploads/presigned-urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "purpose":"COURSE",
                              "images":[{"contentType":"image/jpeg","contentLength":1024}]
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].key").value(matchesPattern("course/1/[0-9a-f-]{36}\\.jpg")))
        }

        @Test
        fun `프리사인 요청은 이미지를 10개까지만 허용한다`() {
            val images =
                (1..11).joinToString(",") {
                    """{"contentType":"image/jpeg","contentLength":1024}"""
                }

            mockMvc
                .perform(
                    post("/api/v1/uploads/presigned-urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"purpose":"PLACE","images":[$images]}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("images"))
        }

        @Test
        fun `프리사인 요청에 지원하지 않는 contentType이면 415를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/uploads/presigned-urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "purpose":"PLACE",
                              "images":[{"contentType":"application/pdf","contentLength":1024}]
                            }
                            """.trimIndent(),
                        ),
                ).andExpect(status().isUnsupportedMediaType)
                .andExpect(jsonPath("$.code").value(4150))
        }

        @Test
        fun `purpose가 없으면 필드 검증 에러를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/uploads/presigned-urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"images":[{"contentType":"image/jpeg","contentLength":1024}]}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `contentLength가 없으면 필드 검증 에러를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/uploads/presigned-urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"purpose":"PROFILE","images":[{"contentType":"image/jpeg"}]}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `contentLength가 0 이하이면 필드 검증 에러를 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/uploads/presigned-urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{"purpose":"PROFILE","images":[{"contentType":"image/jpeg","contentLength":0}]}""",
                        ),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `contentLength가 최대치를 초과하면 413을 내려준다`() {
            mockMvc
                .perform(
                    post("/api/v1/uploads/presigned-urls")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(1L)}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{"purpose":"PROFILE","images":[{"contentType":"image/jpeg","contentLength":10485761}]}""",
                        ),
                ).andExpect(status().isPayloadTooLarge)
                .andExpect(jsonPath("$.code").value(4130))
        }
    }
