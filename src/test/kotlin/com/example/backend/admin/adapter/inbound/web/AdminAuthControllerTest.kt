package com.example.backend.admin.adapter.inbound.web

import com.example.backend.admin.adapter.inbound.web.request.AdminLoginRequest
import com.example.backend.admin.adapter.inbound.web.response.AdminLoginResponse
import com.example.backend.admin.application.port.outbound.AdminIdentityPort
import com.example.backend.admin.application.port.outbound.AdminTokenPort
import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@AutoConfigureMockMvc
@Sql(
    statements =
        [
            "DELETE FROM admins",
            "INSERT INTO admins (email) VALUES ('admin@example.com')",
        ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class AdminAuthControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val adminTokenPort: AdminTokenPort,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @MockitoBean
        private lateinit var adminIdentityPort: AdminIdentityPort

        @Test
        fun `토큰 없이 로그인하면 admins 에 있는 구글 계정에 어드민 토큰을 발급하고 그 토큰으로 me 를 조회한다`() {
            `when`(adminIdentityPort.verifyGoogle("google-id-token")).thenReturn("admin@example.com")

            val body =
                mockMvc
                    .perform(loginRequest())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(2000))
                    .andExpect(jsonPath("$.data.email").value("admin@example.com"))
                    .andExpect(jsonPath("$.data.expiresAt").isNotEmpty)
                    .andReturn()
                    .response.contentAsString
            val accessToken: String = JsonPath.read(body, "$.data.accessToken")

            mockMvc
                .perform(get("/admin/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.email").value("admin@example.com"))
        }

        @Test
        fun `admins 에 없는 구글 계정은 403 4032 로 거부한다`() {
            `when`(adminIdentityPort.verifyGoogle("google-id-token")).thenReturn("someone@example.com")

            mockMvc
                .perform(loginRequest())
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value(4032))
        }

        @Test
        fun `idToken 이 비어 있으면 검증 실패로 거부한다`() {
            mockMvc
                .perform(
                    post("/admin/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"idToken":" "}"""),
                ).andExpect(status().isBadRequest)
        }

        @Test
        fun `어드민 경로는 토큰이 없으면 401 이다`() {
            mockMvc
                .perform(get("/admin/v1/me"))
                .andExpect(status().isUnauthorized)
        }

        @Test
        fun `어드민 경로는 앱 access token 을 거부한다`() {
            mockMvc
                .perform(get("/admin/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer ${appAccessToken()}"))
                .andExpect(status().isForbidden)
        }

        @Test
        fun `앱 개인 경로는 어드민 토큰을 거부한다`() {
            mockMvc
                .perform(get("/api/v1/my/saved-courses").header(HttpHeaders.AUTHORIZATION, "Bearer ${adminToken()}"))
                .andExpect(status().isForbidden)
        }

        @Test
        fun `요청과 응답 DTO 의 toString 은 토큰을 가린다`() {
            val request = AdminLoginRequest(idToken = "secret-id-token")
            val response = AdminLoginResponse("secret-access-token", Instant.EPOCH, "admin@example.com")

            assertFalse(request.toString().contains("secret-id-token"))
            assertFalse(response.toString().contains("secret-access-token"))
        }

        private fun loginRequest() =
            post("/admin/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"idToken":"google-id-token"}""")

        private fun adminToken() = adminTokenPort.issue("admin@example.com").token

        private fun appAccessToken() = jwtTokenProvider.issueAccessToken(1L)
    }
