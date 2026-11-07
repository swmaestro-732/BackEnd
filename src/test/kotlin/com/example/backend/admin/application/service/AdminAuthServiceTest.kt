package com.example.backend.admin.application.service

import com.example.backend.admin.application.port.inbound.dto.AdminLogin
import com.example.backend.admin.application.port.outbound.AdminIdentityPort
import com.example.backend.admin.application.port.outbound.AdminPersistencePort
import com.example.backend.admin.application.port.outbound.AdminTokenPort
import com.example.backend.admin.application.port.outbound.IssuedAdminToken
import com.example.backend.admin.application.port.outbound.VerifiedGoogleAccount
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AdminAuthServiceTest {
    private val issuedFor = mutableListOf<Pair<String, String>>()
    private val checked = mutableListOf<Pair<String, String>>()
    private var verified = VerifiedGoogleAccount(ADMIN_EMAIL, ADMIN_SUB)
    private val registered = mutableSetOf(ADMIN_EMAIL to ADMIN_SUB)

    private val service =
        AdminAuthService(
            adminIdentityPort =
                object : AdminIdentityPort {
                    override fun verifyGoogle(idToken: String) = verified
                },
            adminPersistencePort =
                object : AdminPersistencePort {
                    override fun isRegistered(
                        email: String,
                        googleSub: String,
                    ): Boolean {
                        checked += email to googleSub
                        return (email to googleSub) in registered
                    }
                },
            adminTokenPort =
                object : AdminTokenPort {
                    override fun issue(
                        email: String,
                        googleSub: String,
                    ): IssuedAdminToken {
                        issuedFor += email to googleSub
                        return IssuedAdminToken(token = "admin-token", expiresAt = EXPIRES_AT)
                    }
                },
        )

    @Test
    fun `이메일과 구글 sub 가 함께 등록된 계정이면 어드민 토큰과 만료 시각, 이메일을 돌려준다`() {
        val result = service.login("google-id-token") as AdminLogin.LoggedIn

        assertEquals("admin-token", result.accessToken)
        assertEquals(EXPIRES_AT, result.expiresAt)
        assertEquals(ADMIN_EMAIL, result.email)
        assertEquals(listOf(ADMIN_EMAIL to ADMIN_SUB), issuedFor)
        assertFalse(result.toString().contains("admin-token"))
    }

    @Test
    fun `등록되지 않은 계정은 토큰 없이 등록 필요와 본인 이메일, sub 를 돌려준다`() {
        verified = VerifiedGoogleAccount("someone@example.com", "someone-sub")

        val result = service.login("google-id-token")

        assertEquals(AdminLogin.RegistrationRequired("someone@example.com", "someone-sub"), result)
        assertTrue(issuedFor.isEmpty())
    }

    @Test
    fun `같은 이메일이어도 등록한 구글 계정이 아니면 토큰 없이 등록 필요를 돌려준다`() {
        verified = VerifiedGoogleAccount(ADMIN_EMAIL, "previous-owner-sub")

        val result = service.login("google-id-token")

        assertEquals(AdminLogin.RegistrationRequired(ADMIN_EMAIL, "previous-owner-sub"), result)
        assertTrue(issuedFor.isEmpty())
    }

    @Test
    fun `요청별 어드민 확인은 이메일을 소문자로 바꾸고 등록된 구글 sub 와 비교한다`() {
        assertTrue(service.isAdmin("Admin@Example.COM", ADMIN_SUB))
        assertFalse(service.isAdmin(ADMIN_EMAIL, "previous-owner-sub"))
        assertEquals(listOf(ADMIN_EMAIL to ADMIN_SUB, ADMIN_EMAIL to "previous-owner-sub"), checked)
    }

    private companion object {
        const val ADMIN_EMAIL = "admin@example.com"
        const val ADMIN_SUB = "admin-google-sub"
        val EXPIRES_AT: Instant = Instant.parse("2026-01-01T01:00:00Z")
    }
}
