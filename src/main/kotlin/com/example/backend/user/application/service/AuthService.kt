package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.AuthUseCase
import com.example.backend.user.application.port.inbound.dto.LoginResult
import com.example.backend.user.application.port.inbound.dto.SignupCommand
import com.example.backend.user.application.port.inbound.dto.SignupResult
import com.example.backend.user.application.port.inbound.dto.SignupUserResult
import com.example.backend.user.application.port.inbound.dto.SocialLoginCommand
import com.example.backend.user.application.port.inbound.dto.TokenPair
import com.example.backend.user.application.port.outbound.AuthTokenPort
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.SocialVerificationPort
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.domain.model.User
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
    override fun socialLogin(command: SocialLoginCommand): LoginResult {
        val identity = socialVerificationPort.verify(command.provider, command.idToken)
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
        if (userPersistencePort.existsByNickname(command.nickname)) {
            throw BusinessException(ErrorCode.NICKNAME_ALREADY_TAKEN)
        }
        if (userPersistencePort.existsByHandle(command.handle)) {
            throw BusinessException(ErrorCode.HANDLE_ALREADY_TAKEN)
        }

        val saved =
            userPersistencePort.saveWithSocial(
                User.createWithSocial(
                    nickname = command.nickname,
                    profileImageUrl = command.profileImageUrl,
                    socialProvider = identity.provider,
                    socialId = identity.socialId,
                    handle = command.handle,
                ),
            )
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

    private companion object {
        const val DEV_USER_ID = 1L
    }
}
