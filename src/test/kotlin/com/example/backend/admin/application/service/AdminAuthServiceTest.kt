package com.example.backend.admin.application.service

import com.example.backend.admin.application.port.outbound.AdminIdentityPort
import com.example.backend.admin.application.port.outbound.AdminPersistencePort
import com.example.backend.admin.application.port.outbound.AdminTokenPort
import com.example.backend.admin.application.port.outbound.IssuedAdminToken
import com.example.backend.admin.application.port.outbound.VerifiedGoogleAccount
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.AdminErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class AdminAuthServiceTest {
    private val issuedFor = mutableListOf<String>()
    private val issuedSubs = mutableListOf<String>()
    private val checkedEmails = mutableListOf<String>()
    private var verifiedEmail = ADMIN_EMAIL
    private var verifiedSub = ADMIN_SUB
    private val admins = mutableSetOf(ADMIN_EMAIL)
    private val boundSubs = mutableMapOf<String, String>()

    private val service =
        AdminAuthService(
            adminIdentityPort =
                object : AdminIdentityPort {
                    override fun verifyGoogle(idToken: String) = VerifiedGoogleAccount(verifiedEmail, verifiedSub)
                },
            adminPersistencePort =
                object : AdminPersistencePort {
                    override fun isBound(
                        email: String,
                        googleSub: String,
                    ): Boolean {
                        checkedEmails += email
                        return email in admins && boundSubs[email] == googleSub
                    }

                    override fun bindGoogleSub(
                        email: String,
                        googleSub: String,
                    ): Boolean {
                        checkedEmails += email
                        if (email !in admins) return false
                        return boundSubs.getOrPut(email) { googleSub } == googleSub
                    }
                },
            adminTokenPort =
                object : AdminTokenPort {
                    override fun issue(
                        email: String,
                        googleSub: String,
                    ): IssuedAdminToken {
                        issuedFor += email
                        issuedSubs += googleSub
                        return IssuedAdminToken(token = "admin-token", expiresAt = EXPIRES_AT)
                    }
                },
        )

    @Test
    fun `등록된 어드민이면 어드민 토큰과 만료 시각, 이메일을 돌려준다`() {
        val result = service.login("google-id-token")

        assertEquals("admin-token", result.accessToken)
        assertEquals(EXPIRES_AT, result.expiresAt)
        assertEquals(ADMIN_EMAIL, result.email)
        assertEquals(listOf(ADMIN_EMAIL), issuedFor)
        assertEquals(listOf(ADMIN_SUB), issuedSubs)
        assertFalse(result.toString().contains("admin-token"))
    }

    @Test
    fun `admins 에 없는 계정은 4032 로 거부하고 토큰을 발급하지 않는다`() {
        verifiedEmail = "someone@example.com"

        val exception = assertThrows<BusinessException> { service.login("google-id-token") }

        assertEquals(AdminErrorCode.ADMIN_FORBIDDEN, exception.errorCode)
        assertTrue(issuedFor.isEmpty())
    }

    @Test
    fun `검증된 이메일을 소문자로 바꿔 조회하고 발급한다`() {
        verifiedEmail = "Admin@Example.COM"

        val result = service.login("google-id-token")

        assertEquals(listOf(ADMIN_EMAIL), checkedEmails)
        assertEquals(listOf(ADMIN_EMAIL), issuedFor)
        assertEquals(ADMIN_EMAIL, result.email)
    }

    @Test
    fun `이미 다른 구글 계정에 묶인 어드민 이메일은 4032 로 거부하고 토큰을 발급하지 않는다`() {
        boundSubs[ADMIN_EMAIL] = "current-owner-sub"
        verifiedSub = "previous-owner-sub"

        val exception = assertThrows<BusinessException> { service.login("google-id-token") }

        assertEquals(AdminErrorCode.ADMIN_FORBIDDEN, exception.errorCode)
        assertTrue(issuedFor.isEmpty())
    }

    @Test
    fun `요청별 어드민 확인은 이메일을 소문자로 바꾸고 지금 묶인 구글 sub 와 비교한다`() {
        boundSubs[ADMIN_EMAIL] = ADMIN_SUB

        assertTrue(service.isAdmin("Admin@Example.COM", ADMIN_SUB))
        assertFalse(service.isAdmin(ADMIN_EMAIL, "previous-owner-sub"))
        assertFalse(service.isAdmin("someone@example.com", ADMIN_SUB))
        assertEquals(listOf(ADMIN_EMAIL, ADMIN_EMAIL, "someone@example.com"), checkedEmails)
    }

    private companion object {
        const val ADMIN_EMAIL = "admin@example.com"
        const val ADMIN_SUB = "admin-google-sub"
        val EXPIRES_AT: Instant = Instant.parse("2026-01-01T01:00:00Z")
    }
}
