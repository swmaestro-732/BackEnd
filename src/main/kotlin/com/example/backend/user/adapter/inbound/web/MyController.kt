package com.example.backend.user.adapter.inbound.web

import com.example.backend.bootstrap.security.CurrentUserId
import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.UpdateProfileRequest
import com.example.backend.user.adapter.inbound.web.response.FollowResponse
import com.example.backend.user.adapter.inbound.web.response.MyProfileResponse
import com.example.backend.user.application.port.inbound.MyUseCase
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
 * 인바운드 어댑터 — 현재 로그인 사용자("나") 기준 리소스 (노션 명세 · User).
 *
 * `/api/v1/my` 는 "나" 기준 리소스다: 프로필은 `GET`·`PATCH /my/profile`,
 * 회원 탈퇴는 `DELETE /my`(계정 자체 삭제). 팔로우는 "내 팔로잉" 컬렉션(`/my/followings/{userId}`).
 * 다른 사용자 리소스 자체는 `/users`.
 * 시드 데이터가 없는 개발 환경에서는 `?mock=true` 폴백을 제공한다.
 */
@RestController
@RequestMapping("/api/v1/my")
class MyController(
    private val myUseCase: MyUseCase,
) {
    /** 내 프로필 조회. */
    @GetMapping("/profile")
    fun getMyProfile(
        @CurrentUserId userId: Long,
        @RequestParam(defaultValue = "false") mock: Boolean,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<MyProfileResponse> {
        MockErrors.throwIfRequested(mockError)
        if (mock) return ApiResponse.success(mockProfile())
        return ApiResponse.success(MyProfileResponse.from(myUseCase.getMyProfile(userId)))
    }

    /** 내 프로필 수정. 넘어온 필드만 반영한 결과를 내려준다. */
    @PatchMapping("/profile")
    fun updateMyProfile(
        @CurrentUserId userId: Long,
        @Valid @RequestBody request: UpdateProfileRequest,
        @RequestParam(defaultValue = "false") mock: Boolean,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<MyProfileResponse> {
        MockErrors.throwIfRequested(mockError)
        if (mock) {
            val base = mockProfile()
            return ApiResponse.success(
                base.copy(
                    nickname = request.nickname ?: base.nickname,
                    handle = request.handle ?: base.handle,
                    profileImageUrl = request.profileImageUrl ?: base.profileImageUrl,
                ),
            )
        }
        return ApiResponse.success(MyProfileResponse.from(myUseCase.updateMyProfile(userId, request.toCommand())))
    }

    /** 회원 탈퇴. 본문 없는 성공. */
    @DeleteMapping
    fun withdraw(
        @CurrentUserId userId: Long,
        @RequestParam(defaultValue = "false") mock: Boolean,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<Nothing?> {
        MockErrors.throwIfRequested(mockError)
        if (!mock) myUseCase.withdraw(userId)
        return ApiResponse.ok()
    }

    /** "내 팔로잉"에 대상 사용자를 추가(idempotent). 대상의 팔로워 수를 내려준다. */
    @PutMapping("/followings/{userId}")
    fun follow(
        @CurrentUserId followerId: Long,
        @PathVariable("userId") targetId: Long,
        @RequestParam(defaultValue = "false") mock: Boolean,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<FollowResponse> {
        MockErrors.throwIfRequested(mockError)
        if (mock) return ApiResponse.success(FollowResponse(isFollowing = true, followersCnt = 129))
        return ApiResponse.success(FollowResponse.from(myUseCase.follow(followerId, targetId)))
    }

    /** "내 팔로잉"에서 대상 사용자를 제거. */
    @DeleteMapping("/followings/{userId}")
    fun unfollow(
        @CurrentUserId followerId: Long,
        @PathVariable("userId") targetId: Long,
        @RequestParam(defaultValue = "false") mock: Boolean,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<FollowResponse> {
        MockErrors.throwIfRequested(mockError)
        if (mock) return ApiResponse.success(FollowResponse(isFollowing = false, followersCnt = 128))
        return ApiResponse.success(FollowResponse.from(myUseCase.unfollow(followerId, targetId)))
    }

    private fun mockProfile() =
        MyProfileResponse(
            id = 1L,
            nickname = "현우님",
            handle = "@hyunwoo",
            profileImageUrl = "https://cdn.example.com/users/1.jpg",
            isFollowing = false,
            isFollower = false,
            followersCnt = 128,
            followingsCnt = 88,
            coursesCnt = 12,
        )
}
