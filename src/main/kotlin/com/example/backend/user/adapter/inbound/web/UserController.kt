package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.AccessTokenRequired
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.UpdateProfileRequest
import com.example.backend.user.adapter.inbound.web.response.AccountProfileResponse
import com.example.backend.user.adapter.inbound.web.response.AvailabilityResponse
import com.example.backend.user.adapter.inbound.web.response.FollowResponse
import com.example.backend.user.application.port.inbound.AccountUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — users 리소스(user 도메인). Request → Command, Result → Response 로 매핑한다.
 *
 * 현재 사용자("나")는 JWT 로 식별되므로 식별자 없는 컬렉션 경로(`/api/v1/users`)에 둔다:
 * 내 프로필 수정 `PATCH`, 회원 탈퇴 `DELETE`, 팔로우 `PUT`·`DELETE /followers/{userId}`(대상의 팔로워로 나를 추가·제거).
 * (내 프로필 단독 조회는 마이페이지 `GET /service/v1/mypage` 로 대체 — 중복 제거.)
 * 핸들 가용성은 `GET /availability`(공개). 다른 사용자 프로필은 마이페이지 `GET /service/v1/mypage/{handle}` 로.
 * "나" 기준 핸들러에만 [AccessTokenRequired] 로 access 토큰 인증을 강제한다(그 외는 공개).
 * 시드 데이터가 없는 개발 환경에서는 `?mock=true` 폴백을 제공한다.
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userUseCase: UserUseCase,
    private val accountUseCase: AccountUseCase,
    private val mockGuard: MockGuard,
) {
    // 내 프로필 단독 조회는 제거 — 마이페이지(GET /service/v1/mypage)가 프로필을 포함하므로 중복.

    /** 내 프로필 수정. 넘어온 필드만 반영한 결과를 내려준다. `PATCH /api/v1/users`. */
    @PatchMapping
    @AccessTokenRequired
    fun updateMyProfile(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: UpdateProfileRequest,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<AccountProfileResponse> {
        if (mock && mockGuard.isMockAllowed()) {
            val base = AccountProfileResponse.mock()
            return ApiResponse.success(
                base.copy(
                    nickname = request.nickname ?: base.nickname,
                    handle = request.handle ?: base.handle,
                    profileImageUrl = request.profileImageUrl ?: base.profileImageUrl,
                ),
            )
        }
        return ApiResponse.success(
            AccountProfileResponse.from(
                accountUseCase.updateProfile(userId, request.nickname, request.handle, request.profileImageUrl),
            ),
        )
    }

    // 다른 사용자 프로필 단독 조회(GET /{userId})는 마이페이지(GET /service/v1/mypage/{handle})로 대체돼 제거.
    // (UserUseCase.getProfile 은 코스 상세 화면 BFF 등 내부 조합에서 계속 사용.)

    /**
     * 핸들(아이디) 사용 가능 여부. `GET /api/v1/users/availability?handle=`(공개).
     * 예약어이거나 이미 사용 중이면 available=false.
     */
    @GetMapping("/availability")
    fun checkHandleAvailability(
        @RequestParam @NotBlank handle: String,
    ): ApiResponse<AvailabilityResponse> =
        ApiResponse.success(AvailabilityResponse(available = userUseCase.isHandleAvailable(handle)))

    /**
     * 회원 탈퇴 — 현재 로그인 사용자("나")를 소프트 삭제한다. `DELETE /api/v1/users`.
     * 대상은 JWT 의 나이므로 식별자 없이 컬렉션 경로에 둔다.
     */
    @DeleteMapping
    @AccessTokenRequired
    fun withdraw(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (!(mock && mockGuard.isMockAllowed())) userUseCase.withdraw(userId)
        return ApiResponse.ok()
    }

    /** 대상 사용자의 팔로워로 "나"를 추가(=팔로우, 멱등이라 PUT). 대상의 팔로워 수를 내려준다. */
    @PutMapping("/followers/{userId}")
    @AccessTokenRequired
    fun follow(
        @CurrentUserId followerId: Long,
        @PathVariable("userId") targetId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<FollowResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(FollowResponse.mock(isFollowing = true))
        return ApiResponse.success(FollowResponse.from(accountUseCase.follow(followerId, targetId)))
    }

    /** 대상 사용자의 팔로워에서 "나"를 제거(=언팔로우). */
    @DeleteMapping("/followers/{userId}")
    @AccessTokenRequired
    fun unfollow(
        @CurrentUserId followerId: Long,
        @PathVariable("userId") targetId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<FollowResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(FollowResponse.mock(isFollowing = false))
        return ApiResponse.success(FollowResponse.from(accountUseCase.unfollow(followerId, targetId)))
    }
}
