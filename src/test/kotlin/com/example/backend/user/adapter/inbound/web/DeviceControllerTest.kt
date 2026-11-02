package com.example.backend.user.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.sql.Timestamp
import java.time.Instant

@AutoConfigureMockMvc
class DeviceControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @BeforeEach
        fun setUp() {
            cleanUp()
        }

        @AfterEach
        fun cleanUp() {
            jdbcTemplate.update("DELETE FROM devices WHERE device_id = ?", DEVICE)
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", ME)
        }

        @Test
        fun `로그인 없이 토큰을 보내면 계정 연결 없는 기기 행을 만든다`() {
            mockMvc
                .perform(put("""{"token":"$TOKEN"}""").header("X-App-Platform", "ios").header("X-App-Build", "4"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.marketingAgreedAt").value(null as String?))
                .andExpect(jsonPath("$.data.nightAgreedAt").value(null as String?))

            val row = row()
            assertEquals(TOKEN, row["fcm_token"])
            assertNull(row["user_id"])
            assertEquals("IOS", row["platform"])
            assertEquals(4, row["app_build"])
        }

        @Test
        fun `계정에 연결된 기기에 같은 토큰을 보내면 연결을 유지한다`() {
            jdbcTemplate.update("INSERT INTO users (id, nickname) VALUES (?, '기기주인')", ME)
            jdbcTemplate.update(
                "INSERT INTO devices (device_id, user_id, fcm_token) VALUES (?, ?, ?)",
                DEVICE,
                ME,
                TOKEN,
            )

            mockMvc.perform(put("""{"token":"$TOKEN"}""")).andExpect(status().isOk)

            assertEquals(ME, row()["user_id"])
        }

        @Test
        fun `계정에 연결된 기기의 토큰을 비로그인 경로로 바꾸면 계정 연결을 끊는다`() {
            jdbcTemplate.update("INSERT INTO users (id, nickname) VALUES (?, '기기주인')", ME)
            jdbcTemplate.update("INSERT INTO devices (device_id, user_id, fcm_token) VALUES (?, ?, 'old')", DEVICE, ME)

            mockMvc.perform(put("""{"token":"$TOKEN"}""")).andExpect(status().isOk)

            assertNull(row()["user_id"])
            assertEquals(TOKEN, row()["fcm_token"])
        }

        @Test
        fun `토큰이 있는 기기의 동의는 같은 토큰 없이 바꿀 수 없다`() {
            jdbcTemplate.update("INSERT INTO devices (device_id, fcm_token) VALUES (?, ?)", DEVICE, TOKEN)

            mockMvc
                .perform(put("""{"marketing":true}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
            assertNull(row()["marketing_agreed_at"])

            mockMvc
                .perform(put("""{"token":"$TOKEN","marketing":true}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.marketingAgreedAt").isNotEmpty)
        }

        @Test
        fun `다른 토큰과 동의를 한 요청에 보내면 토큰도 바꾸지 않고 거부한다`() {
            jdbcTemplate.update("INSERT INTO devices (device_id, fcm_token) VALUES (?, ?)", DEVICE, TOKEN)

            mockMvc
                .perform(put("""{"token":"attacker-token","marketing":true}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(TOKEN, row()["fcm_token"])
            assertNull(row()["marketing_agreed_at"])
        }

        @Test
        fun `토큰이 바뀌면 광고, 야간 동의를 지우고 같은 토큰이면 유지한다`() {
            mockMvc.perform(put("""{"token":"$TOKEN","marketing":true,"night":true}""")).andExpect(status().isOk)

            mockMvc.perform(put("""{"token":"$TOKEN"}""")).andExpect(status().isOk)
            assertNotNull(row()["marketing_agreed_at"])

            mockMvc.perform(put("""{"token":"replaced-token"}""")).andExpect(status().isOk)
            assertNull(row()["marketing_agreed_at"])
            assertNull(row()["night_agreed_at"])
        }

        @Test
        fun `토큰 없이 받은 동의는 처음 토큰을 붙일 때 지운다`() {
            mockMvc.perform(put("""{"marketing":true}""")).andExpect(status().isOk)

            mockMvc
                .perform(put("""{"token":"$TOKEN"}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.marketingAgreedAt").value(null as String?))
        }

        @Test
        fun `광고 동의 true는 동의 시각을 응답과 DB에 저장한다`() {
            mockMvc
                .perform(put("""{"marketing":true}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.marketingAgreedAt").isNotEmpty)
                .andExpect(jsonPath("$.data.nightAgreedAt").value(null as String?))

            assertNotNull(row()["marketing_agreed_at"])
            assertNull(row()["night_agreed_at"])
        }

        @Test
        fun `동의 후 다시 동의해도 처음 동의 시각을 유지한다`() {
            mockMvc.perform(put("""{"marketing":true}""")).andExpect(status().isOk)
            val past = Timestamp.from(Instant.parse("2025-01-01T00:00:00Z"))
            jdbcTemplate.update("UPDATE devices SET marketing_agreed_at = ? WHERE device_id = ?", past, DEVICE)

            mockMvc
                .perform(put("""{"marketing":true}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.marketingAgreedAt").value("2025-01-01T00:00:00Z"))

            assertEquals(past, row()["marketing_agreed_at"])
        }

        @Test
        fun `광고 동의 false는 광고와 야간 동의를 모두 지운다`() {
            mockMvc.perform(put("""{"marketing":true,"night":true}""")).andExpect(status().isOk)
            assertNotNull(row()["night_agreed_at"])

            mockMvc
                .perform(put("""{"marketing":false}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.marketingAgreedAt").value(null as String?))
                .andExpect(jsonPath("$.data.nightAgreedAt").value(null as String?))

            assertNull(row()["marketing_agreed_at"])
            assertNull(row()["night_agreed_at"])
        }

        @Test
        fun `광고 동의 없이 야간 동의만 보내면 INVALID_INPUT이고 아무것도 저장하지 않는다`() {
            mockMvc
                .perform(put("""{"night":true}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(0, count())
        }

        @Test
        fun `광고 동의와 야간 동의를 함께 보내면 둘 다 저장한다`() {
            mockMvc
                .perform(put("""{"marketing":true,"night":true}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.marketingAgreedAt").isNotEmpty)
                .andExpect(jsonPath("$.data.nightAgreedAt").isNotEmpty)

            assertNotNull(row()["marketing_agreed_at"])
            assertNotNull(row()["night_agreed_at"])
        }

        @Test
        fun `야간 동의 false는 야간만 지운다`() {
            mockMvc.perform(put("""{"marketing":true,"night":true}""")).andExpect(status().isOk)

            mockMvc.perform(put("""{"night":false}""")).andExpect(status().isOk)

            assertNotNull(row()["marketing_agreed_at"])
            assertNull(row()["night_agreed_at"])
        }

        @Test
        fun `없는 기기에 동의만 보내면 토큰 없는 행을 만든다`() {
            mockMvc.perform(put("""{"marketing":true}""")).andExpect(status().isOk)

            assertEquals(1, count())
            assertNull(row()["fcm_token"])
            assertNull(row()["user_id"])
        }

        @Test
        fun `보내지 않은 필드는 바꾸지 않아 같은 토큰만 보내도 동의가 유지된다`() {
            mockMvc.perform(put("""{"token":"$TOKEN","marketing":true,"night":true}""")).andExpect(status().isOk)

            mockMvc
                .perform(put("""{"token":"$TOKEN"}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.marketingAgreedAt").isNotEmpty)
                .andExpect(jsonPath("$.data.nightAgreedAt").isNotEmpty)

            assertEquals(TOKEN, row()["fcm_token"])
            assertNotNull(row()["marketing_agreed_at"])
            assertNotNull(row()["night_agreed_at"])
        }

        @Test
        fun `GET은 저장된 동의 상태를 돌려주고 로그인 헤더가 필요 없다`() {
            mockMvc.perform(put("""{"marketing":true}""")).andExpect(status().isOk)

            mockMvc
                .perform(get())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.marketingAgreedAt").isNotEmpty)
                .andExpect(jsonPath("$.data.nightAgreedAt").value(null as String?))
        }

        @Test
        fun `GET은 모르는 기기에 미동의를 돌려주고 행을 만들지 않는다`() {
            mockMvc
                .perform(get())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.marketingAgreedAt").value(null as String?))
                .andExpect(jsonPath("$.data.nightAgreedAt").value(null as String?))

            assertEquals(0, count())
        }

        @Test
        fun `X-Device-Id 헤더가 없으면 INVALID_INPUT이다`() {
            mockMvc
                .perform(put("""{"marketing":true}""", deviceId = null))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
            mockMvc
                .perform(get(deviceId = null))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(0, count())
        }

        @Test
        fun `잘못된 토큰은 검증 실패이고 동의도 저장하지 않는다`() {
            mockMvc
                .perform(put("""{"token":"a b","marketing":true}"""))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("token"))

            assertEquals(0, count())
        }

        @Test
        fun `mock 파라미터는 미동의를 돌려주고 아무것도 저장하지 않는다`() {
            mockMvc
                .perform(put("""{"token":"$TOKEN","marketing":true}""").param("mock", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.marketingAgreedAt").value(null as String?))
            mockMvc
                .perform(get().param("mock", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.nightAgreedAt").value(null as String?))

            assertEquals(0, count())
        }

        private fun put(
            body: String,
            deviceId: String? = DEVICE,
        ): MockHttpServletRequestBuilder =
            request(HttpMethod.PUT, PATH).contentType(MediaType.APPLICATION_JSON).content(body).also { b ->
                deviceId?.let { b.header("X-Device-Id", it) }
            }

        private fun get(deviceId: String? = DEVICE): MockHttpServletRequestBuilder =
            request(HttpMethod.GET, PATH).also { b -> deviceId?.let { b.header("X-Device-Id", it) } }

        private fun row(): Map<String, Any?> =
            jdbcTemplate.queryForMap("SELECT * FROM devices WHERE device_id = ?", DEVICE)

        private fun count(): Int =
            jdbcTemplate.queryForObject("SELECT count(*) FROM devices WHERE device_id = ?", Int::class.java, DEVICE)!!

        private companion object {
            const val PATH = "/api/v1/devices"
            const val TOKEN = "device-guest-token"
            const val DEVICE = "fid-guest-device"
            const val ME = 730201L
        }
    }
