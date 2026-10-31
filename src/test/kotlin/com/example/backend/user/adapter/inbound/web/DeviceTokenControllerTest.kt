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
        private val jdbc: JdbcTemplate,
    ) : IntegrationTestBase() {
        @BeforeEach
        fun setUp() {
            cleanUp()
            jdbc.update("INSERT INTO users (id, nickname) VALUES (?, '기기주인'), (?, '다른사용자')", ME, OTHER)
        }

        @AfterEach
        fun cleanUp() {
            jdbc.update("DELETE FROM device_tokens WHERE user_id IN (?, ?)", ME, OTHER)
            jdbc.update("DELETE FROM users WHERE id IN (?, ?)", ME, OTHER)
        }

        @Test
        fun `탈퇴한 사용자는 토큰을 등록할 수 없다`() {
            jdbc.update("UPDATE users SET deleted_at = now() WHERE id = ?", ME)

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
            assertEquals(TOKEN, row["token"])
            assertNull(row["platform"])
            assertNull(row["app_build"])
        }

        @Test
        fun `PUT을 반복해도 토큰 행은 하나만 남는다`() {
            repeat(2) {
                mockMvc.perform(deviceRequest("PUT")).andExpect(status().isOk)
            }

            assertEquals(1, count())
            assertEquals(ME, row()["user_id"])
        }

        @Test
        fun `다른 사용자가 같은 토큰을 등록하면 소유권을 이전한다`() {
            mockMvc.perform(deviceRequest("PUT")).andExpect(status().isOk)
            val id = row()["id"]

            mockMvc.perform(deviceRequest("PUT", userId = OTHER)).andExpect(status().isOk)

            assertEquals(1, count())
            assertEquals(OTHER, row()["user_id"])
            assertEquals(id, row()["id"])
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

        @ParameterizedTest
        @ValueSource(strings = ["PUT", "DELETE"])
        fun `공백 토큰은 검증 실패를 반환한다`(method: String) {
            mockMvc
                .perform(deviceRequest(method, token = "   "))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("token"))

            assertEquals(0, count())
        }

        @ParameterizedTest
        @ValueSource(strings = ["PUT", "DELETE"])
        fun `4096자를 초과한 토큰은 검증 실패를 반환한다`(method: String) {
            mockMvc
                .perform(deviceRequest(method, token = "a".repeat(4097)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("token"))

            assertEquals(0, count())
        }

        @Test
        fun `4096자 토큰은 등록하고 삭제할 수 있다`() {
            val token = "a".repeat(4096)
            mockMvc.perform(deviceRequest("PUT", token)).andExpect(status().isOk)
            assertEquals(token, row(token)["token"])

            mockMvc.perform(deviceRequest("DELETE", token)).andExpect(status().isOk)
            assertEquals(0, count())
        }

        @Test
        fun `DELETE는 자기 토큰을 삭제하고 반복해도 성공한다`() {
            storeToken(ME)

            repeat(2) {
                mockMvc
                    .perform(deviceRequest("DELETE"))
                    .andExpect(status().isOk)
                    .andExpect(content().json("""{"code":2000,"message":null}"""))
                    .andExpect(jsonPath("$.data").doesNotExist())
            }

            assertEquals(0, count())
        }

        @Test
        fun `DELETE는 다른 사용자의 토큰을 삭제하지 않는다`() {
            storeToken(OTHER)

            mockMvc
                .perform(deviceRequest("DELETE"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            assertEquals(OTHER, row()["user_id"])
        }

        @Test
        fun `DELETE는 존재하지 않는 토큰에도 성공한다`() {
            mockMvc
                .perform(deviceRequest("DELETE"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))

            assertEquals(0, count())
        }

        @Test
        fun `소유권 이전 뒤 이전 사용자가 DELETE해도 새 소유자의 토큰은 남는다`() {
            storeToken(ME)
            mockMvc.perform(deviceRequest("PUT", userId = OTHER)).andExpect(status().isOk)

            mockMvc.perform(deviceRequest("DELETE")).andExpect(status().isOk)

            assertEquals(OTHER, row()["user_id"])
        }

        @Test
        fun `DELETE는 유효한 앱 헤더와 함께 자기 토큰을 삭제한다`() {
            storeToken(ME)

            mockMvc
                .perform(deviceRequest("DELETE").header("X-App-Platform", "IOS").header("X-App-Build", "1"))
                .andExpect(status().isOk)

            assertEquals(0, count())
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
        fun `DELETE mock은 성공을 반환하고 토큰을 보존한다`() {
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
                        .content("""{"token":"$TOKEN"}"""),
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
                        .content("""{"token":"$TOKEN"}"""),
                ).andExpect(status().isForbidden)

            assertEquals(0, count())
        }

        private fun deviceRequest(
            method: String,
            token: String = TOKEN,
            userId: Long = ME,
        ): MockHttpServletRequestBuilder =
            request(HttpMethod.valueOf(method), PATH)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(userId)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"$token"}""")

        private fun storeToken(userId: Long) {
            jdbc.update("INSERT INTO device_tokens (user_id, token) VALUES (?, ?)", userId, TOKEN)
        }

        private fun row(token: String = TOKEN): Map<String, Any?> =
            jdbc.queryForMap("SELECT * FROM device_tokens WHERE token = ?", token)

        private fun count(): Int =
            jdbc.queryForObject(
                "SELECT count(*) FROM device_tokens WHERE user_id IN (?, ?)",
                Int::class.java,
                ME,
                OTHER,
            )!!

        private companion object {
            const val PATH = "/api/v1/my/devices"
            const val TOKEN = "device-web-token"
            const val ME = 730001L
            const val OTHER = 730002L
        }
    }
