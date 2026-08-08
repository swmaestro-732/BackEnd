package com.example.backend.user.adapter.inbound.web

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.adapter.inbound.web.request.LogoutRequest
import com.example.backend.user.adapter.inbound.web.request.SignupRequest
import com.example.backend.user.adapter.inbound.web.request.SocialLoginRequest
import com.example.backend.user.adapter.inbound.web.request.TokenReissueRequest
import com.example.backend.user.adapter.inbound.web.response.SignupResponse
import com.example.backend.user.adapter.inbound.web.response.SocialLoginResponse
import com.example.backend.user.adapter.inbound.web.response.TokenResponse
import com.example.backend.user.application.port.inbound.AuthUseCase
import com.example.backend.user.application.port.inbound.dto.SignupCommand
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 인증/세션 (노션 명세 · User/auth).
 *
 * 로그인·회원가입·로그아웃·토큰 재발급 등 **인증 액션**은 `/api/v1/auth` 로 묶는다
 * (`/my` = 내가 기준인 리소스, `/users` = 유저 도메인 리소스와 구분).
 * social-login·signup 은 [AuthUseCase]로 실구현하며, 개발 환경에서 `?mock=true`로 DB 저장 없는
 * 폴백을 제공한다(`?mockError`는 모킹 에러 화면 작업용). 운영 차단은 프로파일 게이팅으로 다룬다.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authUseCase: AuthUseCase,
) {
    /** 소셜 로그인. isNewUser 면 클라이언트가 registrationToken 으로 회원가입을 진행한다. */
    @PostMapping("/social-login")
    fun socialLogin(
        @Valid @RequestBody request: SocialLoginRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<SocialLoginResponse> {
        if (mock) return ApiResponse.success(SocialLoginResponse.mock(authUseCase.issueDevAccessToken()))
        val result = authUseCase.socialLogin(request.provider.toDomain(), request.idToken)
        return ApiResponse.success(SocialLoginResponse.from(result))
    }

    /** 회원가입/프로필 설정. areaCodes 는 user_areas 에, likeThemes(관심 테마)는 user_like_tags 에 저장한다. */
    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody request: SignupRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<SignupResponse> {
        if (mock) {
            return ApiResponse.success(
                SignupResponse.mock(
                    authUseCase.issueDevAccessToken(),
                    request.nickname,
                    request.handle,
                    request.profileImageUrl,
                ),
            )
        }
        val result =
            authUseCase.signup(
                SignupCommand(
                    registrationToken = request.registrationToken,
                    nickname = request.nickname,
                    handle = request.handle,
                    profileImageUrl = request.profileImageUrl,
                    areaCodes = request.areaCodes.orEmpty(),
                    likeThemes = request.likeThemes.orEmpty(),
                ),
            )
        return ApiResponse.success(SignupResponse.from(result))
    }

    /**
     * refresh token 회전으로 accessToken·refreshToken 재발급.
     *
     * `POST /api/v1/auth/refresh`.
     */
    @PostMapping("/refresh")
    fun reissueToken(
        @Valid @RequestBody request: TokenReissueRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<TokenResponse> {
        if (mock) return ApiResponse.success(TokenResponse.mock(authUseCase.issueDevAccessToken()))
        val result = authUseCase.reissue(request.refreshToken)
        return ApiResponse.success(TokenResponse(result.accessToken, result.refreshToken))
    }

    /** refresh token 폐기. 이미 없는 토큰도 성공하는 멱등 요청. */
    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody(required = false) request: LogoutRequest?,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (mock) return ApiResponse.ok()
        val refreshToken = request?.refreshToken ?: throw BusinessException(ErrorCode.INVALID_INPUT)
        authUseCase.logout(refreshToken)
        return ApiResponse.ok()
    }
}
