package com.example.backend.user.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.common.response.UserErrorCode
import com.example.backend.user.application.port.inbound.dto.SignupCommand
import com.example.backend.user.application.port.outbound.AuthTokenPort
import com.example.backend.user.application.port.outbound.IdentityPersistencePort
import com.example.backend.user.application.port.outbound.LikeThemePort
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.RefreshTokenRecord
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.application.port.outbound.SocialVerificationPort
import com.example.backend.user.application.port.outbound.UserAreaPersistencePort
import com.example.backend.user.application.port.outbound.UserLikeThemePort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.application.port.outbound.UserProfileRow
import com.example.backend.user.domain.model.Identity
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import com.example.backend.user.domain.model.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
            var byId: User? = null
            var nicknameExists = false
            var handleExists = false
            var nicknameExcludingExists = false
            var handleExcludingExists = false

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

            override fun existsByNickname(nickname: String): Boolean = nicknameExists

            override fun existsByHandle(handle: String): Boolean = handleExists

            override fun existsByNicknameExcludingUser(
                nickname: String,
                excludeUserId: Long,
            ): Boolean = nicknameExcludingExists

            override fun existsByHandleExcludingUser(
                handle: String,
                excludeUserId: Long,
            ): Boolean = handleExcludingExists

            override fun reactivate(user: User): User = user
        }

    private val identityPersistencePort =
        object : IdentityPersistencePort {
            var activeUser: User? = null
            var withdrawnUser: User? = null

            override fun findActiveUserByCredential(
                provider: SocialProvider,
                socialId: String,
            ): User? = activeUser

            override fun findWithdrawnUserByCredential(
                provider: SocialProvider,
                socialId: String,
            ): User? = withdrawnUser

            override fun register(
                identity: Identity,
                primaryUser: User,
            ): User =
                User.reconstitute(
                    id = 99L,
                    nickname = primaryUser.nickname,
                    handle = primaryUser.handle,
                    profileImageUrl = primaryUser.profileImageUrl,
                )
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
            var revokeResult = false

            override fun issue(userId: Long): String {
                refreshTokenIssued = true
                return "refresh-token"
            }

            override fun findValid(token: String): RefreshTokenRecord? = valid

            override fun revoke(token: String): Boolean = revokeResult

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
            identityPersistencePort = identityPersistencePort,
        )

    // ── socialLogin ─────────────────────────────────────────────────────────────

    @Test
    fun `소셜 로그인 - 활성 사용자는 액세스·리프레시 토큰을 발급한다`() {
        identityPersistencePort.activeUser =
            User.reconstitute(id = 1L, nickname = "활성유저", handle = "active_handle", status = UserStatus.ACTIVE)

        val result = service.socialLogin(SocialProvider.KAKAO, "kakao-token")

        assertThat(result.isNewUser).isFalse()
        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
        assertNull(result.registrationToken)
    }

    @Test
    fun `소셜 로그인 - 신규 사용자는 등록 토큰만 발급한다`() {
        // identityPersistencePort.activeUser = null (기본값)

        val result = service.socialLogin(SocialProvider.KAKAO, "kakao-token")

        assertThat(result.isNewUser).isTrue()
        assertThat(result.registrationToken).isEqualTo("registration-token")
        assertNull(result.accessToken)
        assertNull(result.refreshToken)
    }

    @Test
    fun `정지된 계정은 소셜 로그인 시 ACCOUNT_SUSPENDED 로 거부하고 토큰을 발급하지 않는다`() {
        identityPersistencePort.activeUser =
            User.reconstitute(
                id = 42L,
                nickname = "정지유저",
                handle = "suspended_handle",
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
        identityPersistencePort.activeUser =
            User.reconstitute(
                id = 43L,
                nickname = "대기유저",
                handle = "pending_handle",
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
    fun `WITHDRAWN 계정은 소셜 로그인 시 ACCOUNT_INACTIVE 로 거부한다`() {
        identityPersistencePort.activeUser =
            User.reconstitute(id = 44L, nickname = "탈퇴유저", handle = null, status = UserStatus.WITHDRAWN)

        val ex = assertThrows<BusinessException> { service.socialLogin(SocialProvider.KAKAO, "kakao-token") }

        assertEquals(CommonErrorCode.ACCOUNT_INACTIVE, ex.errorCode)
    }

    @Test
    fun `DELETED 계정은 소셜 로그인 시 ACCOUNT_INACTIVE 로 거부한다`() {
        identityPersistencePort.activeUser =
            User.reconstitute(id = 45L, nickname = "삭제유저", handle = null, status = UserStatus.DELETED)

        val ex = assertThrows<BusinessException> { service.socialLogin(SocialProvider.KAKAO, "kakao-token") }

        assertEquals(CommonErrorCode.ACCOUNT_INACTIVE, ex.errorCode)
    }

    // ── signup ───────────────────────────────────────────────────────────────────

    private fun signupCommand(
        nickname: String = "신규유저",
        handle: String = "new_user",
    ) = SignupCommand(
        registrationToken = "registration-token",
        nickname = nickname,
        handle = handle,
        profileImageUrl = null,
        areaCodes = emptyList(),
        likeThemes = emptyList(),
    )

    @Test
    fun `signup - 신규 사용자를 등록하고 액세스·리프레시 토큰과 사용자 정보를 반환한다`() {
        // activeUser=null, withdrawnUser=null → 신규 가입 경로
        val result = service.signup(signupCommand())

        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
        assertThat(result.user.id).isEqualTo(99L)
        assertThat(result.user.nickname).isEqualTo("신규유저")
        assertThat(result.user.handle).isEqualTo("new_user")
    }

    @Test
    fun `signup - 탈퇴 후 재가입 시 기존 계정을 재활성화하고 토큰을 발급한다`() {
        identityPersistencePort.withdrawnUser =
            User.reconstitute(id = 7L, nickname = "탈퇴유저", handle = null, status = UserStatus.WITHDRAWN)

        val result = service.signup(signupCommand(nickname = "재가입닉", handle = "rejoined"))

        assertNotNull(result.accessToken)
        assertNotNull(result.refreshToken)
        assertThat(result.user.nickname).isEqualTo("재가입닉")
        assertThat(result.user.handle).isEqualTo("rejoined")
    }

    @Test
    fun `signup - 이미 등록된 소셜 계정이면 SOCIAL_ACCOUNT_ALREADY_REGISTERED 를 던진다`() {
        identityPersistencePort.activeUser =
            User.reconstitute(id = 5L, nickname = "기존유저", handle = "existing")

        val ex = assertThrows<BusinessException> { service.signup(signupCommand()) }

        assertEquals(CommonErrorCode.SOCIAL_ACCOUNT_ALREADY_REGISTERED, ex.errorCode)
    }

    @Test
    fun `signup - 신규 가입 시 닉네임이 이미 사용 중이면 NICKNAME_ALREADY_TAKEN 을 던진다`() {
        userPersistencePort.nicknameExists = true

        val ex = assertThrows<BusinessException> { service.signup(signupCommand()) }

        assertEquals(UserErrorCode.NICKNAME_ALREADY_TAKEN, ex.errorCode)
    }

    @Test
    fun `signup - 신규 가입 시 핸들이 이미 사용 중이면 HANDLE_ALREADY_TAKEN 을 던진다`() {
        userPersistencePort.handleExists = true

        val ex = assertThrows<BusinessException> { service.signup(signupCommand()) }

        assertEquals(UserErrorCode.HANDLE_ALREADY_TAKEN, ex.errorCode)
    }

    @Test
    fun `signup - 재가입 시 닉네임이 타인과 중복이면 NICKNAME_ALREADY_TAKEN 을 던진다`() {
        identityPersistencePort.withdrawnUser =
            User.reconstitute(id = 8L, nickname = "탈퇴자", handle = null, status = UserStatus.WITHDRAWN)
        userPersistencePort.nicknameExcludingExists = true

        val ex = assertThrows<BusinessException> { service.signup(signupCommand()) }

        assertEquals(UserErrorCode.NICKNAME_ALREADY_TAKEN, ex.errorCode)
    }

    @Test
    fun `signup - 재가입 시 핸들이 타인과 중복이면 HANDLE_ALREADY_TAKEN 을 던진다`() {
        identityPersistencePort.withdrawnUser =
            User.reconstitute(id = 9L, nickname = "탈퇴자2", handle = null, status = UserStatus.WITHDRAWN)
        userPersistencePort.handleExcludingExists = true

        val ex = assertThrows<BusinessException> { service.signup(signupCommand()) }

        assertEquals(UserErrorCode.HANDLE_ALREADY_TAKEN, ex.errorCode)
    }

    // ── reissue ──────────────────────────────────────────────────────────────────

    @Test
    fun `reissue - 유효한 리프레시 토큰으로 새 토큰 쌍을 발급한다`() {
        refreshTokenPort.valid =
            RefreshTokenRecord(
                id = 1L,
                userId = 10L,
                tokenHash = "hash",
                expiresAt = Instant.EPOCH,
                revoked = false,
                createdAt = Instant.EPOCH,
            )
        userPersistencePort.byId =
            User.reconstitute(id = 10L, nickname = "정상유저", handle = "ok_handle", status = UserStatus.ACTIVE)
        refreshTokenPort.revokeResult = true

        val result = service.reissue("refresh-token")

        assertThat(result.accessToken).isEqualTo("access-token")
        assertThat(result.refreshToken).isEqualTo("refresh-token")
    }

    @Test
    fun `reissue - revoke 가 false 이면 INVALID_REFRESH_TOKEN 을 던진다`() {
        refreshTokenPort.valid =
            RefreshTokenRecord(
                id = 1L,
                userId = 10L,
                tokenHash = "hash",
                expiresAt = Instant.EPOCH,
                revoked = false,
                createdAt = Instant.EPOCH,
            )
        userPersistencePort.byId =
            User.reconstitute(id = 10L, nickname = "정상유저", handle = "ok_handle", status = UserStatus.ACTIVE)
        // revokeResult = false (기본값)

        val ex = assertThrows<BusinessException> { service.reissue("refresh-token") }

        assertEquals(CommonErrorCode.INVALID_REFRESH_TOKEN, ex.errorCode)
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
                status = UserStatus.SUSPENDED,
            )

        val ex = assertThrows<BusinessException> { service.reissue("refresh-token") }

        assertEquals(CommonErrorCode.ACCOUNT_SUSPENDED, ex.errorCode)
        assertFalse(authTokenPort.accessTokenIssued)
        assertFalse(refreshTokenPort.refreshTokenIssued)
    }

    @Test
    fun `reissue - 유효한 토큰 레코드가 없으면 INVALID_REFRESH_TOKEN 을 던진다`() {
        // refreshTokenPort.valid = null (기본값)

        val ex = assertThrows<BusinessException> { service.reissue("stale-token") }

        assertEquals(CommonErrorCode.INVALID_REFRESH_TOKEN, ex.errorCode)
    }

    @Test
    fun `reissue - 토큰은 유효하지만 사용자를 찾지 못하면 INVALID_REFRESH_TOKEN 을 던진다`() {
        refreshTokenPort.valid =
            RefreshTokenRecord(
                id = 2L,
                userId = 999L,
                tokenHash = "hash2",
                expiresAt = Instant.EPOCH,
                revoked = false,
                createdAt = Instant.EPOCH,
            )
        // userPersistencePort.byId = null (기본값)

        val ex = assertThrows<BusinessException> { service.reissue("ghost-token") }

        assertEquals(CommonErrorCode.INVALID_REFRESH_TOKEN, ex.errorCode)
    }

    // ── logout / dev ──────────────────────────────────────────────────────────────

    @Test
    fun `logout 은 예외 없이 완료된다`() {
        service.logout("some-refresh-token")
        // 예외 없이 통과하면 통과
    }

    @Test
    fun `issueDevAccessToken 은 DEV_USER_ID 의 액세스 토큰을 반환한다`() {
        val token = service.issueDevAccessToken()

        assertThat(token).isEqualTo("access-token")
        assertThat(authTokenPort.accessTokenIssued).isTrue()
    }
}
