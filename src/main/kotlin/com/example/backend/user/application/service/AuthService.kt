package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.AuthUseCase
import com.example.backend.user.application.port.inbound.dto.LoginResult
import com.example.backend.user.application.port.inbound.dto.SignupCommand
import com.example.backend.user.application.port.inbound.dto.SignupResult
import com.example.backend.user.application.port.inbound.dto.SignupUserResult
import com.example.backend.user.application.port.inbound.dto.TokenPair
import com.example.backend.user.application.port.outbound.AuthTokenPort
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.SocialVerificationPort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import com.example.backend.user.domain.model.UserStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 소셜 로그인·회원가입 유스케이스 구현. */
@Service
@Transactional(readOnly = true)
class AuthService(
    private val socialVerificationPort: SocialVerificationPort,
    private val userPersistencePort: UserPersistencePort,
    private val authTokenPort: AuthTokenPort,
    private val refreshTokenPort: RefreshTokenPort,
) : AuthUseCase {
    @Transactional
    override fun socialLogin(
        provider: SocialProvider,
        idToken: String,
    ): LoginResult {
        val identity = socialVerificationPort.verify(provider, idToken)
        val user =
            userPersistencePort.findBySocial(identity.provider, identity.socialId)
                ?: return LoginResult(
                    isNewUser = true,
                    registrationToken =
                        authTokenPort.issueRegistrationToken(
                            provider = identity.provider,
                            socialId = identity.socialId,
                        ),
                )

        ensureActive(user)

        val userId = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." }
        return LoginResult(
            accessToken = authTokenPort.issueAccessToken(userId),
            refreshToken = refreshTokenPort.issue(userId),
            isNewUser = false,
        )
    }

    @Transactional
    override fun signup(command: SignupCommand): SignupResult {
        val identity = authTokenPort.parseRegistrationToken(command.registrationToken)
        userPersistencePort.findBySocial(identity.provider, identity.socialId)?.let {
            throw BusinessException(ErrorCode.SOCIAL_ACCOUNT_ALREADY_REGISTERED)
        }
        val withdrawn = userPersistencePort.findWithdrawnBySocial(identity.provider, identity.socialId)
        val saved =
            if (withdrawn != null) {
                val excludeId = checkNotNull(withdrawn.id) { "영속화된 User 는 id 를 가진다." }
                if (userPersistencePort.existsByNicknameExcludingUser(command.nickname, excludeId)) {
                    throw BusinessException(ErrorCode.NICKNAME_ALREADY_TAKEN)
                }
                if (userPersistencePort.existsByHandleExcludingUser(command.handle, excludeId)) {
                    throw BusinessException(ErrorCode.HANDLE_ALREADY_TAKEN)
                }
                userPersistencePort.reactivate(
                    withdrawn.reactivate(command.nickname, command.handle, command.profileImageUrl),
                )
            } else {
                if (userPersistencePort.existsByNickname(command.nickname)) {
                    throw BusinessException(ErrorCode.NICKNAME_ALREADY_TAKEN)
                }
                if (userPersistencePort.existsByHandle(command.handle)) {
                    throw BusinessException(ErrorCode.HANDLE_ALREADY_TAKEN)
                }
                userPersistencePort.saveWithSocial(
                    User.createWithSocial(
                        nickname = command.nickname,
                        profileImageUrl = command.profileImageUrl,
                        socialProvider = identity.provider,
                        socialId = identity.socialId,
                        handle = command.handle,
                    ),
                )
            }
        val userId = checkNotNull(saved.id) { "영속화된 User 는 id 를 가진다." }
        return SignupResult(
            accessToken = authTokenPort.issueAccessToken(userId),
            refreshToken = refreshTokenPort.issue(userId),
            user =
                SignupUserResult(
                    id = userId,
                    nickname = saved.nickname,
                    handle = checkNotNull(saved.handle) { "저장된 User 는 handle 을 가진다." },
                    profileImageUrl = saved.profileImageUrl,
                ),
        )
    }

    @Transactional
    override fun reissue(refreshToken: String): TokenPair {
        val current =
            refreshTokenPort.findValid(refreshToken)
                ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)

        // 토큰 발급 이후 정지·탈퇴된 계정이 재발급으로 세션을 무한 연장하지 못하도록 계정 상태를 재확인한다
        // (socialLogin 과 동일 기준). 계정이 없으면(하드 삭제 등) 토큰 자체를 무효로 본다.
        val user =
            userPersistencePort.findById(current.userId)
                ?: throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        ensureActive(user)

        if (!refreshTokenPort.revoke(refreshToken)) {
            throw BusinessException(ErrorCode.INVALID_REFRESH_TOKEN)
        }
        return TokenPair(
            accessToken = authTokenPort.issueAccessToken(current.userId),
            refreshToken = refreshTokenPort.issue(current.userId),
        )
    }

    @Transactional
    override fun logout(refreshToken: String) {
        refreshTokenPort.revoke(refreshToken)
    }

    override fun isLoginIdTaken(loginId: String): Boolean = userPersistencePort.existsByHandle(loginId)

    override fun issueDevAccessToken(): String = authTokenPort.issueAccessToken(DEV_USER_ID)

    /**
     * 로그인·토큰 재발급 가능한 상태인지 확인한다. 탈퇴/삭제는 deletedAt 필터로 이 지점에 도달하지 않으나,
     * exhaustive when 으로 모든 상태를 명시해 새 상태가 추가되면 컴파일이 막아 누락을 방지한다.
     */
    private fun ensureActive(user: User) {
        when (user.status) {
            UserStatus.ACTIVE -> {
                Unit
            }

            UserStatus.SUSPENDED -> {
                throw BusinessException(ErrorCode.ACCOUNT_SUSPENDED)
            }

            UserStatus.PENDING, UserStatus.WITHDRAWN, UserStatus.DELETED -> {
                throw BusinessException(ErrorCode.ACCOUNT_INACTIVE)
            }
        }
    }

    private companion object {
        const val DEV_USER_ID = 1L
    }
}
