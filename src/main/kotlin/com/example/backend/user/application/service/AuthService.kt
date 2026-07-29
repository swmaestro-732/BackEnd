package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.port.inbound.AuthUseCase
import com.example.backend.user.application.port.inbound.dto.LoginResult
import com.example.backend.user.application.port.inbound.dto.SocialLoginCommand
import com.example.backend.user.application.port.inbound.dto.TokenPair
import com.example.backend.user.application.port.outbound.AuthTokenPort
import com.example.backend.user.application.port.outbound.RefreshTokenPort
import com.example.backend.user.application.port.outbound.SocialVerificationPort
import com.example.backend.user.application.port.outbound.UserPersistencePort
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
    override fun socialLogin(command: SocialLoginCommand): LoginResult {
        val identity = socialVerificationPort.verify(command.provider, command.idToken)

        // 기존 활성 계정이 있으면 로그인. handle 미설정이면 온보딩을 마치지 못한 계정이므로 isNewUser=true.
        userPersistencePort.findBySocial(identity.provider, identity.socialId)?.let { existing ->
            if (existing.status != UserStatus.ACTIVE) throw BusinessException(ErrorCode.ACCOUNT_SUSPENDED)
            val userId = checkNotNull(existing.id) { "영속화된 User 는 id 를 가진다." }
            return LoginResult(
                accessToken = authTokenPort.issueAccessToken(userId),
                refreshToken = refreshTokenPort.issue(userId),
                isNewUser = existing.handle == null,
            )
        }

        // 없거나 탈퇴 상태면 최소 계정(nickname·handle 모두 null)만 만들고 바로 토큰 발급 → 온보딩으로.
        val withdrawn = userPersistencePort.findWithdrawnBySocial(identity.provider, identity.socialId)
        val saved =
            if (withdrawn != null) {
                userPersistencePort.reactivate(withdrawn.reactivate())
            } else {
                userPersistencePort.saveWithSocial(
                    User.createWithSocial(
                        nickname = null,
                        profileImageUrl = null,
                        socialProvider = identity.provider,
                        socialId = identity.socialId,
                        handle = null,
                    ),
                )
            }
        val userId = checkNotNull(saved.id) { "영속화된 User 는 id 를 가진다." }
        return LoginResult(
            accessToken = authTokenPort.issueAccessToken(userId),
            refreshToken = refreshTokenPort.issue(userId),
            isNewUser = true,
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
        if (user.status != UserStatus.ACTIVE) throw BusinessException(ErrorCode.ACCOUNT_SUSPENDED)

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
