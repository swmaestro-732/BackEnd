package com.example.backend.push.adapter.inbound.web

import com.example.backend.admin.application.port.outbound.AdminTokenPort
import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.push.application.port.outbound.PushQueuePort
import com.example.backend.support.IntegrationTestBase
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalTime
import java.time.ZoneId

@AutoConfigureMockMvc
@Sql(
    statements =
        [
            "DELETE FROM push_campaigns",
            "DELETE FROM devices",
            "DELETE FROM admins",
            "INSERT INTO admins (email, google_sub) VALUES ('admin@example.com', 'admin-google-sub')",
            // 야간 동의까지 있어 발송 시각과 무관하게 대상이 된다.
            "INSERT INTO devices (device_id, fcm_token, marketing_agreed_at, night_agreed_at) " +
                "VALUES ('fid-push-admin', 'token-push-admin', now(), now())",
        ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class AdminPushControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val adminTokenPort: AdminTokenPort,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        // 테스트 환경은 SQS 가 없으므로 큐 발행 결과를 테스트마다 정한다. 기본값 false 는 발행 실패다.
        @MockitoBean
        private lateinit var pushQueuePort: PushQueuePort

        @Test
        fun `어드민 토큰으로 발송하면 캠페인을 기록하고 id 와 대상 수와 큐 발행 수를 돌려준다`() {
            // 실제 시계를 쓰므로 발송 금지 시간대(KST 20시 30분부터 21시 전)에는 건너뛴다. 거부 응답은 아래 테스트가 확인한다.
            val kstTime = LocalTime.now(ZoneId.of("Asia/Seoul"))
            if (kstTime >= LocalTime.of(20, 30) && kstTime < LocalTime.of(21, 0)) return
            `when`(pushQueuePort.enqueue(anyArg())).thenReturn(true)

            val body =
                mockMvc
                    .perform(
                        sendRequest(adminTokenPort.issue("admin@example.com", "admin-google-sub").token, validBody),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(2000))
                    .andExpect(jsonPath("$.data.recipientCount").value(1))
                    .andExpect(jsonPath("$.data.enqueuedCount").value(1))
                    .andReturn()
                    .response.contentAsString
            val campaignId = (JsonPath.read<Number>(body, "$.data.campaignId")).toLong()

            val row = jdbcTemplate.queryForMap("SELECT * FROM push_campaigns WHERE id = ?", campaignId)
            assertEquals("admin@example.com", row["admin_email"])
            assertEquals("[커미] 가을 코스", row["title"])
            assertEquals(1, row["recipient_count"])
            assertEquals(1, row["enqueued_count"])

            mockMvc
                .perform(sendRequest(adminTokenPort.issue("admin@example.com", "admin-google-sub").token, validBody))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value(4300))
        }

        @Test
        fun `큐에 하나도 넣지 못하면 500 으로 응답하고 이력을 남기지 않는다`() {
            // 실제 시계를 쓰므로 발송 금지 시간대(KST 20시 30분부터 21시 전)에는 거부 응답을 확인한다.
            val kstTime = LocalTime.now(ZoneId.of("Asia/Seoul"))
            if (kstTime >= LocalTime.of(20, 30) && kstTime < LocalTime.of(21, 0)) {
                mockMvc
                    .perform(
                        sendRequest(adminTokenPort.issue("admin@example.com", "admin-google-sub").token, validBody),
                    ).andExpect(status().isBadRequest)
                    .andExpect(jsonPath("$.code").value(4001))
                return
            }
            mockMvc
                .perform(sendRequest(adminTokenPort.issue("admin@example.com", "admin-google-sub").token, validBody))
                .andExpect(status().isInternalServerError)
                .andExpect(jsonPath("$.code").value(5000))

            assertEquals(0, jdbcTemplate.queryForObject("SELECT count(*) FROM push_campaigns", Int::class.java))
        }

        @Test
        fun `제목이 50자를 넘으면 400 으로 거부한다`() {
            val tooLong = """{"title":"${"가".repeat(51)}","body":"본문","link":"https://example.com"}"""

            mockMvc
                .perform(sendRequest(adminTokenPort.issue("admin@example.com", "admin-google-sub").token, tooLong))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
        }

        @Test
        fun `https 가 아닌 링크는 400 4001 로 거부한다`() {
            val httpLink = """{"title":"가을 코스","body":"본문","link":"http://example.com"}"""

            mockMvc
                .perform(sendRequest(adminTokenPort.issue("admin@example.com", "admin-google-sub").token, httpLink))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `앱 access token 은 403 으로 거부한다`() {
            mockMvc
                .perform(sendRequest(jwtTokenProvider.issueAccessToken(1L), validBody))
                .andExpect(status().isForbidden)
        }

        private val validBody = """{"title":"가을 코스","body":"단풍 코스를 만나보세요","link":"https://example.com/events/1"}"""

        private fun sendRequest(
            token: String,
            content: String,
        ) = post("/admin/v1/pushes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(content)
    }

/** Kotlin 에서 null 검사 없이 쓰는 Mockito any(). mockito-kotlin 과 같은 방식이다. */
@Suppress("UNCHECKED_CAST")
private fun <T> anyArg(): T = org.mockito.Mockito.any<T>() as T
