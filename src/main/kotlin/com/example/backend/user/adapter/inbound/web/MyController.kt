package com.example.backend.user.adapter.inbound.web

import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.UpdateProfileRequest
import com.example.backend.user.adapter.inbound.web.response.FollowResponse
import com.example.backend.user.adapter.inbound.web.response.MyProfileResponse
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
 * 인바운드 어댑터 — 현재 로그인 사용자("나") 기준 리소스 (노션 명세 · User). **모킹 API**.
 *
 * `/api/v1/my` 는 "나" 기준 리소스다: 프로필은 `GET`·`PATCH /my/profile`,
 * 회원 탈퇴는 `DELETE /my`(계정 자체 삭제). 팔로우는 "내 팔로잉" 컬렉션(`/my/followings/{userId}`).
 * 다른 사용자 리소스 자체는 `/users`.
 * 컨트롤러에서 목 데이터를 직접 반환한다. 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고
 * [MockErrors] 호출을 제거한다. `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4040`).
 */
@RestController
@RequestMapping("/api/v1/my")
class MyController {
    /** 내 프로필 조회(모킹). */
    @GetMapping("/profile")
    fun getMyProfile(
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<MyProfileResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(mockProfile())
    }

    /** 내 프로필 수정(모킹). 넘어온 필드만 반영한 결과를 내려준다. */
    @PatchMapping("/profile")
    fun updateMyProfile(
        @Valid @RequestBody request: UpdateProfileRequest,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<MyProfileResponse> {
        MockErrors.throwIfRequested(mockError)
        val base = mockProfile()
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

    /** 팔로우(모킹). "내 팔로잉"에 대상 사용자를 추가(idempotent). 대상의 팔로워 수를 내려준다. */
    @PutMapping("/followings/{userId}")
    fun follow(
        @PathVariable userId: Long,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<FollowResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(FollowResponse(isFollowing = true, followersCnt = 129))
    }

    /** 언팔로우(모킹). "내 팔로잉"에서 대상 사용자를 제거. */
    @DeleteMapping("/followings/{userId}")
    fun unfollow(
        @PathVariable userId: Long,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<FollowResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(FollowResponse(isFollowing = false, followersCnt = 128))
    }

    private fun mockProfile() =
        MyProfileResponse(
            id = MOCK_USER_ID,
            nickname = "현우님",
            handle = "@hyunwoo",
            profileImageUrl = "https://cdn.example.com/users/$MOCK_USER_ID.jpg",
            isFollowing = false,
            isFollower = false,
            followersCnt = 128,
            followingsCnt = 88,
            coursesCnt = 12,
        )

    private companion object {
        const val MOCK_USER_ID = 1L
    }
}
