package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.security.KakaoOauthProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.adapter.inbound.web.request.LogoutRequest
import com.example.backend.user.adapter.inbound.web.request.SignupRequest
import com.example.backend.user.adapter.inbound.web.request.SocialLoginRequest
import com.example.backend.user.adapter.inbound.web.request.TokenReissueRequest
import com.example.backend.user.adapter.inbound.web.response.AvailabilityResponse
import com.example.backend.user.adapter.inbound.web.response.SignupResponse
import com.example.backend.user.adapter.inbound.web.response.SocialLoginResponse
import com.example.backend.user.adapter.inbound.web.response.TokenResponse
import com.example.backend.user.application.port.inbound.AuthUseCase
import com.example.backend.user.application.port.inbound.dto.SignupCommand
import com.example.backend.user.application.port.inbound.dto.SocialLoginCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.GetMapping
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
 * social-login·signup 은 [AuthUseCase]로 실구현하며, Kakao 설정이 없는 개발 환경에서는
 * `?mock=true`로 DB 저장 없는 폴백을 제공한다. `mockError`는 모킹 에러 화면 작업을 위해 유지한다.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authUseCase: AuthUseCase,
    private val kakaoOauthProperties: KakaoOauthProperties,
) {
    /** 소셜 로그인. isNewUser 면 클라이언트가 registrationToken 으로 회원가입을 진행한다. */
    @PostMapping("/social-login")
    fun socialLogin(
        @Valid @RequestBody request: SocialLoginRequest,
        @RequestParam(defaultValue = "false") mock: Boolean,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<SocialLoginResponse> {
        MockErrors.throwIfRequested(mockError)
        if (mock) {
            ensureMockAvailable()
            return ApiResponse.success(
                SocialLoginResponse(
                    accessToken = authUseCase.issueDevAccessToken(),
                    refreshToken = MOCK_REFRESH_TOKEN,
                    isNewUser = false,
                ),
            )
        }
        val result =
            authUseCase.socialLogin(
                SocialLoginCommand(
                    provider = request.provider.toDomain(),
                    idToken = request.idToken,
                ),
            )
        return ApiResponse.success(SocialLoginResponse.from(result))
    }

    /** 회원가입/프로필 설정. handle·areaCodes·likeTagIds 는 아직 저장하지 않는다. */
    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody request: SignupRequest,
        @RequestParam(defaultValue = "false") mock: Boolean,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<SignupResponse> {
        MockErrors.throwIfRequested(mockError)
        if (mock) {
            ensureMockAvailable()
            return ApiResponse.success(
                SignupResponse(
                    accessToken = authUseCase.issueDevAccessToken(),
                    refreshToken = MOCK_REFRESH_TOKEN,
                    user =
                        SignupResponse.SignupUser(
                            id = DEV_USER_ID,
                            nickname = request.nickname,
                            handle = request.handle,
                            profileImageUrl = request.profileImageUrl,
                        ),
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
                ),
            )
        return ApiResponse.success(SignupResponse.from(result))
    }

    /** refresh token 회전으로 accessToken·refreshToken 재발급. */
    @PostMapping("/token-reissue")
    fun reissueToken(
        @Valid @RequestBody request: TokenReissueRequest,
        @RequestParam(defaultValue = "false") mock: Boolean,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<TokenResponse> {
        MockErrors.throwIfRequested(mockError)
        if (mock) {
            ensureMockAvailable()
            return ApiResponse.success(
                TokenResponse(
                    accessToken = authUseCase.issueDevAccessToken(),
                    refreshToken = MOCK_REFRESH_TOKEN,
                ),
            )
        }
        val result = authUseCase.reissue(request.refreshToken)
        return ApiResponse.success(TokenResponse(result.accessToken, result.refreshToken))
    }

    /** refresh token 폐기. 이미 없는 토큰도 성공하는 멱등 요청. */
    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody(required = false) request: LogoutRequest?,
        @RequestParam(defaultValue = "false") mock: Boolean,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<Nothing?> {
        MockErrors.throwIfRequested(mockError)
        if (mock) {
            ensureMockAvailable()
            return ApiResponse.ok()
        }
        val refreshToken = request?.refreshToken ?: throw BusinessException(ErrorCode.INVALID_INPUT)
        authUseCase.logout(refreshToken)
        return ApiResponse.ok()
    }

    /**
     * 아이디(핸들) 사용 가능 여부. 예약어와 이미 사용 중인 값은 제외한다.
     *
     * Deprecated: 인증 플로우가 아니라 users 리소스 조회이므로 `GET /api/v1/users/availability?handle=` 로 대체한다.
     * 신·구 병행 유지 중 — 클라이언트 전환 완료 후 이 엔드포인트와 [RESERVED_LOGIN_IDS]·[AuthUseCase.isLoginIdTaken] 를 제거한다.
     */
    @GetMapping("/login-id/availability")
    fun checkLoginIdAvailability(
        @RequestParam @NotBlank loginId: String,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<AvailabilityResponse> {
        MockErrors.throwIfRequested(mockError)
        val available = loginId.lowercase() !in RESERVED_LOGIN_IDS && !authUseCase.isLoginIdTaken(loginId)
        return ApiResponse.success(AvailabilityResponse(available = available))
    }

    private fun ensureMockAvailable() {
        if (kakaoOauthProperties.clientId.isNotBlank()) {
            throw BusinessException(ErrorCode.SOCIAL_AUTHENTICATION_FAILED)
        }
    }

    private companion object {
        const val MOCK_REFRESH_TOKEN = "mock-refresh-token"
        const val DEV_USER_ID = 1L

        /** 사용 불가로 내려줄 예약어. */
        val RESERVED_LOGIN_IDS = setOf("admin", "courmy", "test")
    }
}
