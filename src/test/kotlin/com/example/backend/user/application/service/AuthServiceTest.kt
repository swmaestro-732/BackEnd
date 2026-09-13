package com.example.backend.user.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.UserErrorCode
import com.example.backend.user.application.port.inbound.dto.SignupCommand
import com.example.backend.user.application.port.outbound.AuthTokenPort
import com.example.backend.user.application.port.outbound.LikeThemePort
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.RefreshTokenRecord
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.application.port.outbound.SocialVerificationPort
import com.example.backend.user.application.port.outbound.UserAreaPersistencePort
import com.example.backend.user.application.port.outbound.UserLikeThemePort
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

            override fun findAll(): List<User> = emptyList()

            override fun findById(id: Long): User? = byId

            override fun lockActive(userIds: List<Long>): Set<Long> = userIds.toSet()

            override fun findByHandle(handle: String): User? = null

            override fun findProfile(userId: Long): UserProfileRow? = null

            override fun findProfiles(userIds: List<Long>): List<UserProfileRow> = emptyList()

            override fun save(user: User): User = user

            override fun update(user: User) = Unit

            override fun applyCourseCountDelta(
                userId: Long,
                publicDelta: Int,
                followerDelta: Int,
                privateDelta: Int,
            ) = Unit

            override fun softDelete(user: User) = Unit

            var nicknameExists = false
            var handleExists = false

            override fun existsByNickname(nickname: String): Boolean = nicknameExists

            override fun existsByHandle(handle: String): Boolean = handleExists

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
            var valid: RefreshTokenRecord? = null

            override fun issue(userId: Long): String {
                refreshTokenIssued = true
                return "refresh-token"
            }

            override fun findValid(token: String): RefreshTokenRecord? = valid

            override fun revoke(token: String): Boolean = false

            override fun revokeAllByUser(userId: Long) = Unit
        }

    private val userAreaPersistencePort =
        object : UserAreaPersistencePort {
            override fun replaceAreas(
                userId: Long,
                areaCodes: List<String>,
            ) = Unit

            override fun findAreaCodes(userId: Long): List<String> = emptyList()
        }

    private val areaQueryUseCase =
        object : AreaQueryUseCase {
            override fun searchAreas(keyword: String): List<AreaDescriptor> = emptyList()

            override fun findAreaByCode(code: String): AreaDescriptor? = null
        }

    private val userLikeThemePort =
        object : UserLikeThemePort {
            override fun replaceLikeThemes(
                userId: Long,
                themes: List<String>,
            ) = Unit

            override fun findLikeThemes(userId: Long): List<String> = emptyList()
        }

    // 관심 테마 정본은 course 도메인 소유라, user 테스트는 enum 을 참조하지 않고 이름만 흉내낸다.
    private val likeThemePort =
        object : LikeThemePort {
            override fun listThemeNames(): List<String> = listOf("DATE", "CAFETOUR", "CULTURE")
        }

    private val service =
        AuthService(
            socialVerificationPort = socialVerificationPort,
            userPersistencePort = userPersistencePort,
            userAreaPersistencePort = userAreaPersistencePort,
            userLikeThemePort = userLikeThemePort,
            authTokenPort = authTokenPort,
            refreshTokenPort = refreshTokenPort,
            userAreaResolver = UserAreaResolver(areaQueryUseCase),
            userLikeThemeResolver = UserLikeThemeResolver(likeThemePort),
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
                service.socialLogin(SocialProvider.KAKAO, "kakao-token")
            }

        assertEquals(CommonErrorCode.ACCOUNT_SUSPENDED, ex.errorCode)
        assertFalse(authTokenPort.accessTokenIssued)
        assertFalse(refreshTokenPort.refreshTokenIssued)
    }

    @Test
    fun `정지가 아닌 비활성(PENDING) 계정은 소셜 로그인 시 ACCOUNT_INACTIVE 로 거부한다`() {
        userPersistencePort.bySocial =
            User.reconstitute(
                id = 43L,
                nickname = "대기유저",
                handle = "pending_handle",
                socialProvider = identity.provider,
                socialId = identity.socialId,
                status = UserStatus.PENDING,
            )

        val ex =
            assertThrows<BusinessException> {
                service.socialLogin(SocialProvider.KAKAO, "kakao-token")
            }

        assertEquals(CommonErrorCode.ACCOUNT_INACTIVE, ex.errorCode)
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

        assertEquals(CommonErrorCode.ACCOUNT_SUSPENDED, ex.errorCode)
        assertFalse(authTokenPort.accessTokenIssued)
        assertFalse(refreshTokenPort.refreshTokenIssued)
    }

    @Test
    fun `신규 소셜 유저(미가입)는 로그인 시 registrationToken 과 isNewUser=true 를 받는다`() {
        // bySocial defaults to null → new user branch
        val result = service.socialLogin(SocialProvider.KAKAO, "kakao-token")

        assertTrue(result.isNewUser)
        assertEquals("registration-token", result.registrationToken)
        assertNull(result.accessToken)
        assertNull(result.refreshToken)
    }

    @Test
    fun `기존 활성 유저는 소셜 로그인 시 accessToken 과 refreshToken 을 받는다`() {
        userPersistencePort.bySocial =
            User.reconstitute(
                id = 1L,
                nickname = "활성유저",
                handle = "active_handle",
                socialProvider = identity.provider,
                socialId = identity.socialId,
                status = UserStatus.ACTIVE,
            )

        val result = service.socialLogin(SocialProvider.KAKAO, "kakao-token")

        assertFalse(result.isNewUser)
        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
        assertNull(result.registrationToken)
        assertTrue(authTokenPort.accessTokenIssued)
        assertTrue(refreshTokenPort.refreshTokenIssued)
    }

    @Test
    fun `reissue — 유효하지 않은 refreshToken 은 INVALID_REFRESH_TOKEN 을 던진다`() {
        // valid defaults to null → token not found
        val ex = assertThrows<BusinessException> { service.reissue("unknown-token") }

        assertEquals(CommonErrorCode.INVALID_REFRESH_TOKEN, ex.errorCode)
    }

    @Test
    fun `reissue — refreshToken 은 유효하나 userId 에 해당하는 유저가 없으면 INVALID_REFRESH_TOKEN 을 던진다`() {
        refreshTokenPort.valid =
            RefreshTokenRecord(
                id = 2L,
                userId = 99L,
                tokenHash = "h",
                expiresAt = Instant.EPOCH,
                revoked = false,
                createdAt = Instant.EPOCH,
            )
        // byId defaults to null

        val ex = assertThrows<BusinessException> { service.reissue("refresh-token") }

        assertEquals(CommonErrorCode.INVALID_REFRESH_TOKEN, ex.errorCode)
    }

    @Test
    fun `reissue — revoke 가 실패하면 INVALID_REFRESH_TOKEN 을 던진다`() {
        refreshTokenPort.valid =
            RefreshTokenRecord(
                id = 3L,
                userId = 1L,
                tokenHash = "h",
                expiresAt = Instant.EPOCH,
                revoked = false,
                createdAt = Instant.EPOCH,
            )
        userPersistencePort.byId =
            User.reconstitute(
                id = 1L,
                nickname = "유저",
                handle = "user_handle",
                socialProvider = identity.provider,
                socialId = identity.socialId,
                status = UserStatus.ACTIVE,
            )
        // refreshTokenPort.revoke always returns false (fake) → throws

        val ex = assertThrows<BusinessException> { service.reissue("refresh-token") }

        assertEquals(CommonErrorCode.INVALID_REFRESH_TOKEN, ex.errorCode)
    }

    @Test
    fun `logout — refreshToken 을 폐기하고 예외가 발생하지 않는다`() {
        service.logout("some-refresh-token") // no exception
    }

    @Test
    fun `issueDevAccessToken — DEV 사용자 accessToken 을 반환한다`() {
        val token = service.issueDevAccessToken()

        assertNotNull(token)
        assertEquals("access-token", token)
    }

    @Test
    fun `signup - 이미 가입된 소셜 계정이면 SOCIAL_ACCOUNT_ALREADY_REGISTERED 를 던진다`() {
        userPersistencePort.bySocial =
            User.reconstitute(
                id = 1L,
                nickname = "기존유저",
                handle = "existing_handle",
                socialProvider = identity.provider,
                socialId = identity.socialId,
                status = UserStatus.ACTIVE,
            )

        val ex =
            assertThrows<BusinessException> {
                service.signup(
                    SignupCommand(
                        registrationToken = "reg-token",
                        nickname = "신규닉네임",
                        handle = "new_handle",
                        profileImageUrl = null,
                    ),
                )
            }

        assertEquals(CommonErrorCode.SOCIAL_ACCOUNT_ALREADY_REGISTERED, ex.errorCode)
    }

    @Test
    fun `signup - 닉네임이 중복이면 NICKNAME_ALREADY_TAKEN 을 던진다`() {
        userPersistencePort.nicknameExists = true

        val ex =
            assertThrows<BusinessException> {
                service.signup(
                    SignupCommand(
                        registrationToken = "reg-token",
                        nickname = "중복닉네임",
                        handle = "unique_handle",
                        profileImageUrl = null,
                    ),
                )
            }

        assertEquals(UserErrorCode.NICKNAME_ALREADY_TAKEN, ex.errorCode)
    }

    @Test
    fun `signup - handle 이 중복이면 HANDLE_ALREADY_TAKEN 을 던진다`() {
        userPersistencePort.handleExists = true

        val ex =
            assertThrows<BusinessException> {
                service.signup(
                    SignupCommand(
                        registrationToken = "reg-token",
                        nickname = "unique_nickname",
                        handle = "중복핸들",
                        profileImageUrl = null,
                    ),
                )
            }

        assertEquals(UserErrorCode.HANDLE_ALREADY_TAKEN, ex.errorCode)
    }
}
