package com.example.backend.admin.adapter.inbound.web

import com.example.backend.admin.adapter.inbound.web.request.AdminLoginRequest
import com.example.backend.admin.adapter.inbound.web.response.AdminLoginResponse
import com.example.backend.admin.application.port.outbound.AdminIdentityPort
import com.example.backend.admin.application.port.outbound.AdminTokenPort
import com.example.backend.admin.application.port.outbound.VerifiedGoogleAccount
import com.example.backend.bootstrap.security.AdminJwtTokenIssuer
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
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
            "INSERT INTO admins (email, google_sub) VALUES ('admin@example.com', 'admin-google-sub')",
        ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class AdminAuthControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val adminTokenPort: AdminTokenPort,
        private val jwtTokenProvider: JwtTokenProvider,
        private val jdbcTemplate: JdbcTemplate,
        private val jwtEncoder: JwtEncoder,
    ) : IntegrationTestBase() {
        @MockitoBean
        private lateinit var adminIdentityPort: AdminIdentityPort

        @Test
        fun `토큰 없이 로그인하면 admins 에 있는 구글 계정에 어드민 토큰을 발급하고 그 토큰으로 me 를 조회한다`() {
            `when`(
                adminIdentityPort.verifyGoogle("google-id-token"),
            ).thenReturn(VerifiedGoogleAccount("admin@example.com", ADMIN_SUB))

            val body =
                mockMvc
                    .perform(loginRequest())
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(2000))
                    .andExpect(jsonPath("$.data.result").value("LOGGED_IN"))
                    .andExpect(jsonPath("$.data.email").value("admin@example.com"))
                    .andExpect(jsonPath("$.data.expiresAt").isNotEmpty)
                    .andExpect(jsonPath("$.data.googleSub").doesNotExist())
                    .andReturn()
                    .response.contentAsString
            val accessToken: String = JsonPath.read(body, "$.data.accessToken")

            mockMvc
                .perform(get("/admin/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.email").value("admin@example.com"))
        }

        @Test
        fun `같은 이메일이어도 등록한 구글 계정이 아니면 토큰 없이 등록 필요와 본인 sub 를 돌려준다`() {
            `when`(adminIdentityPort.verifyGoogle("google-id-token"))
                .thenReturn(VerifiedGoogleAccount("admin@example.com", "previous-owner-sub"))

            mockMvc
                .perform(loginRequest())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.result").value("REGISTRATION_REQUIRED"))
                .andExpect(jsonPath("$.data.email").value("admin@example.com"))
                .andExpect(jsonPath("$.data.googleSub").value("previous-owner-sub"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist())
        }

        @Test
        fun `admins 에 없는 구글 계정은 토큰 없이 등록 필요와 운영자에게 전달할 이메일과 sub 를 돌려준다`() {
            `when`(
                adminIdentityPort.verifyGoogle("google-id-token"),
            ).thenReturn(VerifiedGoogleAccount("someone@example.com", "other-sub"))

            mockMvc
                .perform(loginRequest())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.result").value("REGISTRATION_REQUIRED"))
                .andExpect(jsonPath("$.data.email").value("someone@example.com"))
                .andExpect(jsonPath("$.data.googleSub").value("other-sub"))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
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
        fun `admins 에 남아 있는 어드민의 토큰은 어드민 경로를 통과한다`() {
            mockMvc
                .perform(get("/admin/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer ${adminToken()}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.email").value("admin@example.com"))
        }

        @Test
        fun `등록 계정(google_sub)을 바꾸면 이전 계정의 만료 전 토큰은 즉시 403 이다`() {
            val previousToken = adminToken()
            jdbcTemplate.update("UPDATE admins SET google_sub = 'new-owner-sub' WHERE email = 'admin@example.com'")

            mockMvc
                .perform(get("/admin/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer $previousToken"))
                .andExpect(status().isForbidden)
            mockMvc
                .perform(
                    get("/admin/v1/me")
                        .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer ${adminTokenPort.issue("admin@example.com", "new-owner-sub").token}",
                        ),
                ).andExpect(status().isOk)
        }

        @Test
        fun `admins 에서 삭제된 어드민은 만료 전 토큰이어도 즉시 403 이다`() {
            val token = adminToken()
            jdbcTemplate.update("DELETE FROM admins WHERE email = ?", "admin@example.com")

            mockMvc
                .perform(get("/admin/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
                .andExpect(status().isForbidden)
        }

        @Test
        fun `어드민 경로는 앱 access token 을 거부한다`() {
            mockMvc
                .perform(get("/admin/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer ${appAccessToken()}"))
                .andExpect(status().isForbidden)
        }

        @Test
        fun `purpose 가 admin 이 아니면 이메일과 google_sub 가 맞아도 어드민 경로는 403 이다`() {
            val token = signedToken(purpose = "access", googleSub = ADMIN_SUB)

            mockMvc
                .perform(get("/admin/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
                .andExpect(status().isForbidden)
        }

        @Test
        fun `google_sub 클레임이 없는 어드민 토큰은 403 이다`() {
            val token = signedToken(purpose = "admin", googleSub = null)

            mockMvc
                .perform(get("/admin/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer $token"))
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
            val response =
                AdminLoginResponse(
                    AdminLoginResponse.Result.LOGGED_IN,
                    "admin@example.com",
                    "secret-access-token",
                    Instant.EPOCH,
                    null,
                )

            assertFalse(request.toString().contains("secret-id-token"))
            assertFalse(response.toString().contains("secret-access-token"))
        }

        private fun loginRequest() =
            post("/admin/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"idToken":"google-id-token"}""")

        private fun adminToken() = adminTokenPort.issue("admin@example.com", ADMIN_SUB).token

        private fun appAccessToken() = jwtTokenProvider.issueAccessToken(1L)

        /** 서버 키로 서명하되 purpose 와 google_sub 를 마음대로 정한 토큰. 인가 조건을 하나씩 떼어 확인한다. */
        private fun signedToken(
            purpose: String,
            googleSub: String?,
        ): String {
            val now = Instant.now()
            val claims =
                JwtClaimsSet
                    .builder()
                    .subject("admin@example.com")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(600))
                    .claim("purpose", purpose)
                    .apply { if (googleSub != null) claim(AdminJwtTokenIssuer.GOOGLE_SUB_CLAIM, googleSub) }
                    .build()
            val header = JwsHeader.with(MacAlgorithm.HS256).build()
            return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        }

        private companion object {
            const val ADMIN_SUB = "admin-google-sub"
        }
    }
