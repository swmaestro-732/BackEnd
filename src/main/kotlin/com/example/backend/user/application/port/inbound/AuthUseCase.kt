package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.LoginResult
import com.example.backend.user.application.port.inbound.dto.SignupCommand
import com.example.backend.user.application.port.inbound.dto.SignupResult
import com.example.backend.user.application.port.inbound.dto.TokenPair
import com.example.backend.user.domain.model.SocialProvider

/** 인증 액션을 제공하는 인바운드 포트. */
interface AuthUseCase {
    // 파라미터가 4개 이하면 커맨드로 감싸지 않고 그대로 받는다(팀 컨벤션).
    fun socialLogin(
        provider: SocialProvider,
        idToken: String,
    ): LoginResult

    fun signup(command: SignupCommand): SignupResult

    fun reissue(refreshToken: String): TokenPair

    fun logout(refreshToken: String)

    fun isLoginIdTaken(loginId: String): Boolean

    /** Kakao 설정이 없는 개발 환경의 mock 폴백용 access token. */
    fun issueDevAccessToken(): String
}
