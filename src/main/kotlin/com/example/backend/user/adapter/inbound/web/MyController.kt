package com.example.backend.user.adapter.inbound.web

import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.SignupRequest
import com.example.backend.user.adapter.inbound.web.request.SocialLoginRequest
import com.example.backend.user.adapter.inbound.web.request.TokenReissueRequest
import com.example.backend.user.adapter.inbound.web.request.UpdateProfileRequest
import com.example.backend.user.adapter.inbound.web.response.AvailabilityResponse
import com.example.backend.user.adapter.inbound.web.response.MyProfileResponse
import com.example.backend.user.adapter.inbound.web.response.SignupResponse
import com.example.backend.user.adapter.inbound.web.response.SocialLoginResponse
import com.example.backend.user.adapter.inbound.web.response.TokenResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 로그인 사용자 기반 인증·세션·프로필 (노션 명세 · User). **모킹 API**.
 *
 * 컨트롤러에서 목 데이터를 직접 만들어 반환한다. 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고
 * [MockErrors] 호출을 제거한다. `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4040`).
 * 토큰은 JWT 실구현 전이라 더미 문자열이다.
 */
@RestController
@RequestMapping("/api/v1/my")
class MyController {
    /** 소셜 로그인(모킹). isNewUser 면 클라이언트가 회원가입(프로필 설정)으로 분기한다. */
    @PostMapping("/social-login")
    fun socialLogin(
        @Valid @RequestBody request: SocialLoginRequest,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<SocialLoginResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(
            SocialLoginResponse(
                accessToken = MOCK_ACCESS_TOKEN,
                refreshToken = MOCK_REFRESH_TOKEN,
                isNewUser = false,
            ),
        )
    }

    /** 회원가입/프로필 설정(모킹). 가입 완료 후 토큰과 생성된 유저 요약을 내려준다. */
    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody request: SignupRequest,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<SignupResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(
            SignupResponse(
                accessToken = MOCK_ACCESS_TOKEN,
                refreshToken = MOCK_REFRESH_TOKEN,
                user =
                    SignupResponse.SignupUser(
                        id = MOCK_USER_ID,
                        nickname = request.nickname,
                        handle = request.handle,
                        profileImageUrl = request.profileImageUrl,
                    ),
            ),
        )
    }

    /** accessToken 재발급(모킹). */
    @PostMapping("/token-reissue")
    fun reissueToken(
        @Valid @RequestBody request: TokenReissueRequest,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<TokenResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(
            TokenResponse(accessToken = MOCK_ACCESS_TOKEN, refreshToken = MOCK_REFRESH_TOKEN),
        )
    }

    /** 로그아웃(모킹). 본문 없는 성공. */
    @PostMapping("/logout")
    fun logout(
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<Nothing?> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.ok()
    }

    /** 아이디(핸들) 사용 가능 여부(모킹). 예약어는 사용 불가로 내려 현실감 있게. */
    @GetMapping("/login-id/availability")
    fun checkLoginIdAvailability(
        @RequestParam @NotBlank loginId: String,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<AvailabilityResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(AvailabilityResponse(available = loginId.lowercase() !in RESERVED_LOGIN_IDS))
    }

    /** 프로필 조회(모킹). */
    @GetMapping("/{userId}")
    fun getProfile(
        @PathVariable userId: Long,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<MyProfileResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(mockProfile(userId))
    }

    /** 프로필 수정(모킹). 넘어온 필드만 반영한 결과를 내려준다. */
    @PatchMapping("/profile")
    fun updateProfile(
        @Valid @RequestBody request: UpdateProfileRequest,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<MyProfileResponse> {
        MockErrors.throwIfRequested(mockError)
        val base = mockProfile(MOCK_USER_ID)
        return ApiResponse.success(
            base.copy(
                nickname = request.nickname ?: base.nickname,
                handle = request.handle ?: base.handle,
                profileImageUrl = request.profileImageUrl ?: base.profileImageUrl,
            ),
        )
    }

    /** 회원 탈퇴(모킹). 본문 없는 성공. */
    @DeleteMapping
    fun withdraw(
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<Nothing?> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.ok()
    }

    private fun mockProfile(userId: Long) =
        MyProfileResponse(
            id = userId,
            nickname = "현우님",
            handle = "@hyunwoo",
            profileImageUrl = "https://cdn.example.com/users/$userId.jpg",
            isFollowing = false,
            isFollower = false,
            followersCnt = 128,
            followingsCnt = 88,
            coursesCnt = 12,
        )

    private companion object {
        const val MOCK_ACCESS_TOKEN = "mock-access-token"
        const val MOCK_REFRESH_TOKEN = "mock-refresh-token"
        const val MOCK_USER_ID = 1L

        /** 사용 불가로 내려줄 예약어(모킹) — 실제 중복 검사 대신. */
        val RESERVED_LOGIN_IDS = setOf("admin", "courmy", "test")
    }
}
