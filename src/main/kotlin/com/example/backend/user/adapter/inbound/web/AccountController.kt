package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.mock.MockGuard
import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.UpdateProfileRequest
import com.example.backend.user.adapter.inbound.web.response.AccountProfileResponse
import com.example.backend.user.adapter.inbound.web.response.FollowResponse
import com.example.backend.user.application.port.inbound.AccountUseCase
import com.example.backend.user.application.port.inbound.UserUseCase
import jakarta.validation.Valid
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
 * 인바운드 어댑터 — 현재 로그인 사용자("나") 기준 계정 리소스 (노션 명세 · User).
 *
 * "나" 기준 리소스다: 프로필은 `GET`·`PATCH /profile`, 회원 탈퇴는 `DELETE`(계정 자체 삭제).
 * 팔로우는 "내 팔로잉" 컬렉션(`/followings/{userId}`). 다른 사용자 리소스 자체는 `/users`.
 * 시드 데이터가 없는 개발 환경에서는 `?mock=true` 폴백을 제공한다.
 *
 * `/service/v1` 별칭이 정식이며 `/api/v1/my` 는 deprecated 별칭 — 동일 핸들러다.
 * (BFF는 mobile.* 패키지가 원칙이나, 이 응답/요청 DTO는 user.adapter 소속이라 ArchUnit상 mobile 패키지로 옮기면
 *  DTO를 복제해야 한다. 중복 없이 동일 핸들러를 재사용하고자 다중 경로 매핑으로 둔다.)
 */
@RestController
@RequestMapping(value = ["/service/v1", "/api/v1/my"])
class AccountController(
    private val accountUseCase: AccountUseCase,
    private val userUseCase: UserUseCase,
    private val mockGuard: MockGuard,
) {
    /** 내 프로필 조회. */
    @GetMapping("/profile")
    fun getMyProfile(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<AccountProfileResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(AccountProfileResponse.mock())
        return ApiResponse.success(AccountProfileResponse.from(accountUseCase.getProfile(userId)))
    }

    /** 내 프로필 수정. 넘어온 필드만 반영한 결과를 내려준다. */
    @PatchMapping("/profile")
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

    /**
     * 회원 탈퇴. 본문 없는 성공.
     * Deprecated: 회원 탈퇴는 user 도메인 리소스라 정식 경로는 `DELETE /api/v1/users/me`.
     * 이 경로(`DELETE /service/v1`·`/api/v1/my`)는 마이그레이션용 별칭 — 동일 유스케이스([UserUseCase.withdraw]).
     */
    @DeleteMapping
    fun withdraw(
        @CurrentUserId userId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<Nothing?> {
        if (!(mock && mockGuard.isMockAllowed())) userUseCase.withdraw(userId)
        return ApiResponse.ok()
    }

    /** "내 팔로잉"에 대상 사용자를 추가(idempotent). 대상의 팔로워 수를 내려준다. */
    @PutMapping("/followings/{userId}")
    fun follow(
        @CurrentUserId followerId: Long,
        @PathVariable("userId") targetId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<FollowResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(FollowResponse.mock(isFollowing = true))
        return ApiResponse.success(FollowResponse.from(accountUseCase.follow(followerId, targetId)))
    }

    /** "내 팔로잉"에서 대상 사용자를 제거. */
    @DeleteMapping("/followings/{userId}")
    fun unfollow(
        @CurrentUserId followerId: Long,
        @PathVariable("userId") targetId: Long,
        @RequestParam(required = false) mock: Boolean = false,
    ): ApiResponse<FollowResponse> {
        if (mock && mockGuard.isMockAllowed()) return ApiResponse.success(FollowResponse.mock(isFollowing = false))
        return ApiResponse.success(FollowResponse.from(accountUseCase.unfollow(followerId, targetId)))
    }
}
