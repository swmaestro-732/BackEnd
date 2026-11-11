package com.example.backend.mobile.aichat.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.jayway.jsonpath.JsonPath
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * `POST /service/v1/ai-chat/messages` 통합 테스트 — 인증·검증·에러 코드와 세션 생성·이어가기, 해석 실패 시 같은 단계 재안내를 검증한다.
 * 테스트 환경엔 지역 마스터 데이터와 검색 엔진이 없어 지역 단계를 넘어가는 흐름은 [AiChatServiceTest] 가 맡는다.
 * 공유 컨텍스트를 그대로 쓴다 — 빈을 목으로 바꾸면 컨텍스트가 하나 더 떠서 다른 테스트의 DataSource 가 닫히는 문제가 있다.
 */
@AutoConfigureMockMvc
class AiChatControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        private fun send(
            body: String,
            token: String? = bearer(1L),
        ): ResultActions =
            mockMvc.perform(
                post(PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .apply { token?.let { header(HttpHeaders.AUTHORIZATION, it) } },
            )

        @Test
        fun `토큰이 없으면 401 이다`() {
            send("""{"type":"TEXT","text":"성수"}""", token = null).andExpect(status().isUnauthorized)
        }

        @Test
        fun `type 이 없거나 CHIP 에 value 가 없으면 4002 다`() {
            send("""{"text":"성수"}""")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
            send("""{"type":"CHIP","text":"성수"}""")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `현재 단계에 없는 칩은 4001 이다`() {
            send("""{"type":"CHIP","value":"DATE"}""")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `모르는 sessionId 는 4400 이다`() {
            send("""{"sessionId":"00000000-0000-0000-0000-000000000000","type":"CHIP","value":"DATE"}""")
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4400))
                .andExpect(jsonPath("$.message").value("대화가 만료되었습니다. 다시 시작해 주세요."))
        }

        @Test
        fun `알 수 없는 지역은 세션을 만들고 성공 응답으로 같은 단계를 칩 없이 다시 묻는다`() {
            val body =
                send("""{"sessionId":null,"type":"TEXT","text":"아틀란티스","location":null}""")
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(2000))
                    .andExpect(jsonPath("$.data.step").value("REGION"))
                    .andExpect(jsonPath("$.data.sessionId").isNotEmpty)
                    .andExpect(jsonPath("$.data.messages[0].text").value(containsString("찾지 못했어요")))
                    .andExpect(jsonPath("$.data.quickReplies.length()").value(0))
                    .andExpect(jsonPath("$.data.input.enabled").value(true))
                    .andExpect(jsonPath("$.data.input.placeholder").isNotEmpty)
                    .andExpect(jsonPath("$.data.courses.length()").value(0))
                    .andReturn()
                    .response.contentAsString
            val sessionId: String = JsonPath.read(body, "$.data.sessionId")

            // 같은 세션으로 이어 보내면 세션이 유지되고, 다른 사용자가 보내면 없는 세션으로 취급한다.
            send("""{"sessionId":"$sessionId","type":"TEXT","text":"아틀란티스"}""")
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.step").value("REGION"))
            send("""{"sessionId":"$sessionId","type":"TEXT","text":"아틀란티스"}""", token = bearer(2L))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4400))
        }

        @Test
        fun `내 근처는 좌표가 없으면 지역을 다시 묻는다`() {
            send("""{"type":"CHIP","value":"NEAR_ME","location":null}""")
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.step").value("REGION"))
                .andExpect(jsonPath("$.data.messages[0].text").value(containsString("위치 권한")))
        }

        @Test
        fun `좌표 범위를 벗어나면 4002 다`() {
            send("""{"type":"CHIP","value":"NEAR_ME","location":{"latitude":91.0,"longitude":127.0}}""")
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        private fun bearer(userId: Long) = "Bearer ${jwtTokenProvider.issueAccessToken(userId)}"

        private companion object {
            const val PATH = "/service/v1/ai-chat/messages"
        }
    }
