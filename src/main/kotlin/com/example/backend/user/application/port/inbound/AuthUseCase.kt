package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.LoginResult
import com.example.backend.user.application.port.inbound.dto.SignupCommand
import com.example.backend.user.application.port.inbound.dto.SignupResult
import com.example.backend.user.application.port.inbound.dto.SocialLoginCommand

/** 인증 액션을 제공하는 인바운드 포트. */
interface AuthUseCase {
    fun socialLogin(command: SocialLoginCommand): LoginResult

    fun signup(command: SignupCommand): SignupResult

    /** Kakao 설정이 없는 개발 환경의 mock 폴백용 access token. */
    fun issueDevAccessToken(): String
}
