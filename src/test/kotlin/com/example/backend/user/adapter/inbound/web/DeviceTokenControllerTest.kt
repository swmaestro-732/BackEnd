package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.domain.model.SocialProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class DeviceTokenControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @BeforeEach
        fun setUp() {
            cleanUp()
            jdbcTemplate.update("INSERT INTO users (id, nickname) VALUES (?, '기기주인'), (?, '다른사용자')", ME, OTHER)
        }

        @AfterEach
        fun cleanUp() {
            jdbcTemplate.update("DELETE FROM devices WHERE device_id = ? OR user_id IN (?, ?)", DEVICE, ME, OTHER)
            jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", ME, OTHER)
        }

        @Test
        fun `이전 앱 빌드가 보내는 token 필드도 받는다`() {
            mockMvc
                .perform(
                    request(HttpMethod.PUT, PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(ME)}")
                        .header("X-Device-Id", DEVICE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"token":"$TOKEN"}"""),
                ).andExpect(status().isOk)

            assertEquals(TOKEN, row()["fcm_token"])
        }

        @Test
        fun `탈퇴한 사용자는 토큰을 등록할 수 없다`() {
            jdbcTemplate.update("UPDATE users SET deleted_at = now() WHERE id = ?", ME)

            mockMvc
                .perform(deviceRequest("PUT"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4042))

            assertEquals(0, count())
        }

        @Test
        fun `PUT은 토큰을 저장하고 데이터 없는 성공 응답을 반환한다`() {
            mockMvc
                .perform(deviceRequest("PUT"))
                .andExpect(status().isOk)
                .andExpect(content().json("""{"code":2000,"message":null}"""))
                .andExpect(jsonPath("$.data").doesNotExist())

            val row = row()
            assertEquals(ME, row["user_id"])
            assertEquals(TOKEN, row["fcm_token"])
            assertNull(row["platform"])
            assertNull(row["app_build"])
        }

        @Test
        fun `PUT을 반복해도 기기 행은 하나만 남는다`() {
            repeat(2) {
                mockMvc.perform(deviceRequest("PUT")).andExpect(status().isOk)
            }

            assertEquals(1, count())
            assertEquals(ME, row()["user_id"])
        }

        @Test
        fun `같은 기기에서 같은 토큰으로 다른 사용자가 등록하면 같은 행의 연결이 새 사용자로 옮겨진다`() {
            mockMvc.perform(deviceRequest("PUT")).andExpect(status().isOk)

            mockMvc.perform(deviceRequest("PUT", userId = OTHER)).andExpect(status().isOk)

            assertEquals(1, count())
            assertEquals(OTHER, row()["user_id"])
        }

        @Test
        fun `PUT은 플랫폼과 빌드를 헤더에서 읽고 재등록 때 갱신한다`() {
            mockMvc
                .perform(deviceRequest("PUT").header("X-App-Platform", "android").header("X-App-Build", "1"))
                .andExpect(status().isOk)
            assertEquals("ANDROID", row()["platform"])
            assertEquals(1, row()["app_build"])

            mockMvc
                .perform(deviceRequest("PUT").header("X-App-Platform", "iOs").header("X-App-Build", "12"))
                .andExpect(status().isOk)

            assertEquals("IOS", row()["platform"])
            assertEquals(12, row()["app_build"])
        }

        @Test
        fun `헤더 없이 재등록하면 기존 플랫폼과 빌드를 null로 갱신한다`() {
            mockMvc
                .perform(deviceRequest("PUT").header("X-App-Platform", "ANDROID").header("X-App-Build", "1"))
                .andExpect(status().isOk)

            mockMvc.perform(deviceRequest("PUT")).andExpect(status().isOk)

            assertNull(row()["platform"])
            assertNull(row()["app_build"])
        }

        @Test
        fun `플랫폼 헤더만 있으면 빌드는 null로 저장한다`() {
            mockMvc
                .perform(deviceRequest("PUT").header("X-App-Platform", "ANDROID"))
                .andExpect(status().isOk)

            assertEquals("ANDROID", row()["platform"])
            assertNull(row()["app_build"])
        }

        @Test
        fun `빌드 헤더만 있으면 플랫폼은 null로 저장한다`() {
            mockMvc.perform(deviceRequest("PUT").header("X-App-Build", "1")).andExpect(status().isOk)

            assertNull(row()["platform"])
            assertEquals(1, row()["app_build"])
        }

        @ParameterizedTest
        @ValueSource(strings = ["abc", "", "2147483648"])
        fun `잘못된 빌드 헤더는 INVALID_APP_HEADER이고 저장하지 않는다`(build: String) {
            mockMvc
                .perform(deviceRequest("PUT").header("X-App-Build", build))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4005))

            assertEquals(0, count())
        }

        @ParameterizedTest
        @ValueSource(strings = ["WEB", "", " android "])
        fun `잘못된 플랫폼 헤더는 INVALID_APP_HEADER이고 저장하지 않는다`(platform: String) {
            mockMvc
                .perform(deviceRequest("PUT").header("X-App-Platform", platform))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4005))

            assertEquals(0, count())
        }

        @Test
        fun `기기 ID가 같은 다른 사용자가 등록하면 기기 행을 이어받고 이후 원래 사용자가 다시 가져온다`() {
            storeToken(ME)

            mockMvc.perform(deviceRequest("PUT", token = "other-token", userId = OTHER)).andExpect(status().isOk)
            assertEquals(1, count())
            assertEquals(OTHER, row()["user_id"])
            assertEquals("other-token", row()["fcm_token"])

            mockMvc.perform(deviceRequest("PUT", token = "owner-new-token")).andExpect(status().isOk)
            assertEquals(1, count())
            assertEquals(ME, row()["user_id"])
            assertEquals("owner-new-token", row()["fcm_token"])
        }

        @Test
        fun `다른 기기가 같은 토큰을 등록하면 이전 기기 행은 토큰과 연결을 비우고 남는다`() {
            mockMvc.perform(deviceRequest("PUT")).andExpect(status().isOk)

            mockMvc
                .perform(deviceRequest("PUT", userId = OTHER, deviceId = "fid-web-device-2"))
                .andExpect(status().isOk)

            val old = row()
            assertNull(old["fcm_token"])
            assertNull(old["user_id"])
            val taken = jdbcTemplate.queryForMap("SELECT * FROM devices WHERE device_id = 'fid-web-device-2'")
            assertEquals(TOKEN, taken["fcm_token"])
            assertEquals(OTHER, taken["user_id"])
        }

        @Test
        fun `Android 가 보내는 platform, appBuild 필드가 body 에 있어도 무시하고 헤더 값을 저장한다`() {
            mockMvc
                .perform(
                    deviceRequest("PUT")
                        .header("X-App-Platform", "ANDROID")
                        .header("X-App-Build", "7")
                        .content("""{"fcmToken":"$TOKEN","platform":"IOS","appBuild":99}"""),
                ).andExpect(status().isOk)

            assertEquals(TOKEN, row()["fcm_token"])
            assertEquals("ANDROID", row()["platform"])
            assertEquals(7, row()["app_build"])
        }

        @Test
        fun `같은 기기에서 토큰이 바뀌면 행 하나에서 토큰만 교체한다`() {
            mockMvc.perform(deviceRequest("PUT", token = "old-token")).andExpect(status().isOk)
            mockMvc.perform(deviceRequest("PUT", token = "new-token")).andExpect(status().isOk)

            assertEquals(1, count())
            assertEquals("new-token", row()["fcm_token"])
        }

        @ParameterizedTest
        @ValueSource(strings = ["PUT", "DELETE"])
        fun `X-Device-Id 헤더가 없으면 INVALID_INPUT이다`(method: String) {
            storeToken(ME)

            mockMvc
                .perform(deviceRequest(method, deviceId = null))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(TOKEN, row()["fcm_token"])
        }

        @ParameterizedTest
        @ValueSource(strings = ["   ", "129"])
        fun `X-Device-Id 헤더가 비었거나 128자를 넘으면 INVALID_INPUT이다`(value: String) {
            val deviceId = if (value == "129") "a".repeat(129) else value
            mockMvc
                .perform(deviceRequest("PUT", deviceId = deviceId))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))

            assertEquals(0, count())
        }

        @Test
        fun `공백 토큰은 검증 실패를 반환한다`() {
            mockMvc
                .perform(deviceRequest("PUT", token = "   "))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("fcmToken"))

            assertEquals(0, count())
        }

        @Test
        fun `1024자를 초과한 토큰은 검증 실패를 반환한다`() {
            mockMvc
                .perform(deviceRequest("PUT", token = "a".repeat(1025)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("fcmToken"))

            assertEquals(0, count())
        }

        @ParameterizedTest
        @ValueSource(strings = ["한", "😀", "a b"])
        fun `ASCII가 아니거나 공백이 섞인 토큰은 검증 실패이고 저장하지 않는다`(unit: String) {
            mockMvc
                .perform(deviceRequest("PUT", token = unit.repeat(10)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("fcmToken"))

            assertEquals(0, count())
        }

        @Test
        fun `1024자 토큰은 등록하고 연결을 끊을 수 있다`() {
            val token = "a".repeat(1024)
            mockMvc.perform(deviceRequest("PUT", token)).andExpect(status().isOk)
            assertEquals(token, row()["fcm_token"])

            mockMvc.perform(deviceRequest("DELETE")).andExpect(status().isOk)
            assertNull(row()["user_id"])
            assertEquals(token, row()["fcm_token"])
        }

        @Test
        fun `DELETE는 자기 기기의 연결만 끊고 토큰은 남기며 반복해도 성공한다`() {
            storeToken(ME)

            repeat(2) {
                mockMvc
                    .perform(deviceRequest("DELETE"))
                    .andExpect(status().isOk)
                    .andExpect(content().json("""{"code":2000,"message":null}"""))
                    .andExpect(jsonPath("$.data").doesNotExist())
            }

            assertNull(row()["user_id"])
            assertEquals(TOKEN, row()["fcm_token"])
        }

        @Test
        fun `DELETE는 다른 사용자의 기기 연결을 끊지 않는다`() {
            storeToken(OTHER)

            mockMvc
                .perform(deviceRequest("DELETE"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            assertEquals(OTHER, row()["user_id"])
        }

        @Test
        fun `DELETE는 존재하지 않는 기기에도 성공한다`() {
            mockMvc
                .perform(deviceRequest("DELETE"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            assertEquals(0, count())
        }

        @Test
        fun `연결이 넘어간 뒤 이전 사용자가 DELETE해도 새 사용자의 연결은 남는다`() {
            storeToken(ME)
            mockMvc.perform(deviceRequest("PUT", userId = OTHER)).andExpect(status().isOk)

            mockMvc.perform(deviceRequest("DELETE")).andExpect(status().isOk)

            assertEquals(OTHER, row()["user_id"])
        }

        @Test
        fun `DELETE는 유효한 앱 헤더와 함께 자기 기기의 연결을 끊는다`() {
            storeToken(ME)

            mockMvc
                .perform(deviceRequest("DELETE").header("X-App-Platform", "IOS").header("X-App-Build", "1"))
                .andExpect(status().isOk)

            assertNull(row()["user_id"])
        }

        @Test
        fun `PUT mock은 성공을 반환하고 저장하지 않는다`() {
            mockMvc
                .perform(deviceRequest("PUT").param("mock", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            assertEquals(0, count())
        }

        @Test
        fun `DELETE mock은 성공을 반환하고 연결을 보존한다`() {
            storeToken(ME)

            mockMvc
                .perform(deviceRequest("DELETE").param("mock", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            assertEquals(ME, row()["user_id"])
        }

        @ParameterizedTest
        @ValueSource(strings = ["PUT", "DELETE"])
        fun `인증 토큰이 없으면 401을 반환한다`(method: String) {
            mockMvc
                .perform(
                    request(HttpMethod.valueOf(method), PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"fcmToken":"$TOKEN"}"""),
                ).andExpect(status().isUnauthorized)

            assertEquals(0, count())
        }

        @ParameterizedTest
        @ValueSource(strings = ["PUT", "DELETE"])
        fun `회원가입 토큰으로는 접근할 수 없다`(method: String) {
            val registrationToken = jwtTokenProvider.issueRegistrationToken(SocialProvider.KAKAO, "device-social")
            mockMvc
                .perform(
                    request(HttpMethod.valueOf(method), PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer $registrationToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"fcmToken":"$TOKEN"}"""),
                ).andExpect(status().isForbidden)

            assertEquals(0, count())
        }

        private fun deviceRequest(
            method: String,
            token: String = TOKEN,
            userId: Long = ME,
            deviceId: String? = DEVICE,
        ): MockHttpServletRequestBuilder {
            val builder =
                request(HttpMethod.valueOf(method), PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(userId)}")
            deviceId?.let { builder.header("X-Device-Id", it) }
            if (method == "PUT") builder.contentType(MediaType.APPLICATION_JSON).content("""{"fcmToken":"$token"}""")
            return builder
        }

        private fun storeToken(userId: Long) {
            jdbcTemplate.update(
                "INSERT INTO devices (user_id, device_id, fcm_token) VALUES (?, ?, ?)",
                userId,
                DEVICE,
                TOKEN,
            )
        }

        private fun row(): Map<String, Any?> =
            jdbcTemplate.queryForMap("SELECT * FROM devices WHERE device_id = ?", DEVICE)

        private fun count(): Int =
            jdbcTemplate.queryForObject("SELECT count(*) FROM devices WHERE device_id = ?", Int::class.java, DEVICE)!!

        private companion object {
            const val PATH = "/api/v1/my/devices"
            const val TOKEN = "device-web-token"
            const val DEVICE = "fid-web-device"
            const val ME = 730001L
            const val OTHER = 730002L
        }
    }
