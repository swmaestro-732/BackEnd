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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

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
            var byId: User? = null
            var withdrawn: User? = null
            var savedWithSocial: User? = null
            var reactivated: User? = null

            override fun findAll(): List<User> = emptyList()

            override fun findById(id: Long): User? = byId

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
            ): User? = withdrawn

            override fun existsByNicknameExcludingUser(
                nickname: String,
                excludeUserId: Long,
            ): Boolean = false

            override fun existsByHandleExcludingUser(
                handle: String,
                excludeUserId: Long,
            ): Boolean = false

            // 신규 최소계정 저장 — 영속화된 것처럼 id 를 부여해 돌려준다(createWithSocial 은 id=null).
            override fun saveWithSocial(user: User): User {
                savedWithSocial = user
                return User.reconstitute(
                    id = 100L,
                    nickname = user.nickname,
                    handle = user.handle,
                    profileImageUrl = user.profileImageUrl,
                    socialProvider = user.socialProvider,
                    socialId = user.socialId,
                    status = user.status,
                )
            }

            override fun reactivate(user: User): User {
                reactivated = user
                return user
            }
        }

    private val authTokenPort =
        object : AuthTokenPort {
            var accessTokenIssued = false

            override fun issueAccessToken(userId: Long): String {
                accessTokenIssued = true
                return "access-token"
            }
        }

    private val refreshTokenPort =
        object : RefreshTokenPort {
            var refreshTokenIssued = false
            var valid: RefreshTokenRecord? = null

            override fun issue(userId: Long): String {
                refreshTokenIssued = true
                return "refresh-token"
            }

            override fun findValid(token: String): RefreshTokenRecord? = valid

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

    @Test
    fun `정지된 계정은 토큰 재발급 시 ACCOUNT_SUSPENDED 로 거부하고 새 토큰을 발급하지 않는다`() {
        refreshTokenPort.valid =
            RefreshTokenRecord(
                id = 1L,
                userId = 42L,
                tokenHash = "hash",
                expiresAt = Instant.EPOCH,
                revoked = false,
                createdAt = Instant.EPOCH,
            )
        userPersistencePort.byId =
            User.reconstitute(
                id = 42L,
                nickname = "정지유저",
                handle = "suspended_handle",
                socialProvider = identity.provider,
                socialId = identity.socialId,
                status = UserStatus.SUSPENDED,
            )

        val ex = assertThrows<BusinessException> { service.reissue("refresh-token") }

        assertEquals(ErrorCode.ACCOUNT_SUSPENDED, ex.errorCode)
        assertFalse(authTokenPort.accessTokenIssued)
        assertFalse(refreshTokenPort.refreshTokenIssued)
    }

    @Test
    fun `온보딩 완료(핸들 보유) 기존 계정은 로그인하고 isNewUser 는 false`() {
        userPersistencePort.bySocial =
            User.reconstitute(
                id = 7L,
                nickname = "현우",
                handle = "hyunwoo",
                socialProvider = identity.provider,
                socialId = identity.socialId,
                status = UserStatus.ACTIVE,
            )

        val result = service.socialLogin(SocialLoginCommand(provider = SocialProvider.KAKAO, idToken = "kakao-token"))

        assertFalse(result.isNewUser)
        assertEquals("access-token", result.accessToken)
        assertTrue(authTokenPort.accessTokenIssued)
    }

    @Test
    fun `핸들 미설정(온보딩 미완료) 기존 계정은 isNewUser 가 true`() {
        userPersistencePort.bySocial =
            User.reconstitute(
                id = 7L,
                nickname = null,
                handle = null,
                socialProvider = identity.provider,
                socialId = identity.socialId,
                status = UserStatus.ACTIVE,
            )

        val result = service.socialLogin(SocialLoginCommand(provider = SocialProvider.KAKAO, idToken = "kakao-token"))

        assertTrue(result.isNewUser)
    }

    @Test
    fun `신규 소셜 계정은 최소계정(핸들 없음)을 만들고 isNewUser 는 true`() {
        // bySocial=null, withdrawn=null → 신규 생성
        val result = service.socialLogin(SocialLoginCommand(provider = SocialProvider.KAKAO, idToken = "kakao-token"))

        assertTrue(result.isNewUser)
        assertNotNull(userPersistencePort.savedWithSocial)
        assertNull(userPersistencePort.savedWithSocial?.handle)
        assertNull(userPersistencePort.savedWithSocial?.nickname)
    }

    @Test
    fun `탈퇴 후 재로그인은 최소상태로 재활성화하고 isNewUser 는 true`() {
        userPersistencePort.withdrawn =
            User.reconstitute(
                id = 9L,
                nickname = "옛닉",
                handle = "old_handle",
                socialProvider = identity.provider,
                socialId = identity.socialId,
                status = UserStatus.WITHDRAWN,
            )

        val result = service.socialLogin(SocialLoginCommand(provider = SocialProvider.KAKAO, idToken = "kakao-token"))

        assertTrue(result.isNewUser)
        assertNotNull(userPersistencePort.reactivated)
        // reactivate() 는 온보딩 리셋 — 핸들·닉네임이 비워진다.
        assertNull(userPersistencePort.reactivated?.handle)
        assertNull(userPersistencePort.reactivated?.nickname)
    }
}
