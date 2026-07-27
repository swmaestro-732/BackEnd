package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.dto.SocialLoginCommand
import com.example.backend.user.application.port.outbound.AuthTokenPort
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.RefreshTokenRecord
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.application.port.outbound.SocialVerificationPort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.application.port.outbound.UserProfileRow
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import com.example.backend.user.domain.model.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * AuthService 순수 단위 테스트 — 실제 소셜 검증(Kakao)이 필요한 경로는 아웃바운드 포트를 손수 만든 fake 로 대체한다.
 * (통합 테스트로는 Kakao 검증을 구동할 수 없어 서비스 계층에서 검증한다.)
 */
class AuthServiceTest {
    private val identity = SocialIdentity(provider = SocialProvider.KAKAO, socialId = "kakao-123")

    private val socialVerificationPort =
        object : SocialVerificationPort {
            override fun verify(
                provider: SocialProvider,
                idToken: String,
            ): SocialIdentity = identity
        }

    private val userPersistencePort =
        object : UserPersistencePort {
            var bySocial: User? = null

            override fun findAll(): List<User> = emptyList()

            override fun findById(id: Long): User? = null

            override fun findProfile(userId: Long): UserProfileRow? = null

            override fun save(user: User): User = user

            override fun update(user: User) = Unit

            override fun softDelete(user: User) = Unit

            override fun existsByNickname(nickname: String): Boolean = false

            override fun existsByHandle(handle: String): Boolean = false

            override fun findBySocial(
                provider: SocialProvider,
                socialId: String,
            ): User? = bySocial

            override fun findWithdrawnBySocial(
                provider: SocialProvider,
                socialId: String,
            ): User? = null

            override fun existsByNicknameExcludingUser(
                nickname: String,
                excludeUserId: Long,
            ): Boolean = false

            override fun existsByHandleExcludingUser(
                handle: String,
                excludeUserId: Long,
            ): Boolean = false

            override fun saveWithSocial(user: User): User = user

            override fun reactivate(user: User): User = user
        }

    private val authTokenPort =
        object : AuthTokenPort {
            var accessTokenIssued = false

            override fun issueAccessToken(userId: Long): String {
                accessTokenIssued = true
                return "access-token"
            }

            override fun issueRegistrationToken(
                provider: SocialProvider,
                socialId: String,
            ): String = "registration-token"

            override fun parseRegistrationToken(token: String): SocialIdentity = identity
        }

    private val refreshTokenPort =
        object : RefreshTokenPort {
            var refreshTokenIssued = false

            override fun issue(userId: Long): String {
                refreshTokenIssued = true
                return "refresh-token"
            }

            override fun findValid(token: String): RefreshTokenRecord? = null

            override fun revoke(token: String): Boolean = false

            override fun revokeAllByUser(userId: Long) = Unit
        }

    private val service =
        AuthService(
            socialVerificationPort = socialVerificationPort,
            userPersistencePort = userPersistencePort,
            authTokenPort = authTokenPort,
            refreshTokenPort = refreshTokenPort,
        )

    @Test
    fun `정지된 계정은 소셜 로그인 시 ACCOUNT_SUSPENDED 로 거부하고 토큰을 발급하지 않는다`() {
        userPersistencePort.bySocial =
            User.reconstitute(
                id = 42L,
                nickname = "정지유저",
                handle = "suspended_handle",
                socialProvider = identity.provider,
                socialId = identity.socialId,
                status = UserStatus.SUSPENDED,
            )

        val ex =
            assertThrows<BusinessException> {
                service.socialLogin(SocialLoginCommand(provider = SocialProvider.KAKAO, idToken = "kakao-token"))
            }

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.errorCode)
        assertFalse(authTokenPort.accessTokenIssued)
        assertFalse(refreshTokenPort.refreshTokenIssued)
    }
}
