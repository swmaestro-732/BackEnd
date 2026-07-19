package com.example.backend.user.adapter.inbound.web

import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.response.FollowResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — 팔로우/언팔로우 (노션 명세 · User). **모킹 API**.
 *
 * 실구현(UserController)과 경로 도메인(`/api/v1/users`)은 같지만 목업이라 별도 컨트롤러로 분리했다.
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고 [MockErrors] 호출을 제거한다.
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4040`).
 */
@RestController
@RequestMapping("/api/v1/users")
class FollowController {
    /** 팔로우(모킹). 팔로우 후 상태 + 대상의 팔로워 수를 내려준다. */
    @PostMapping("/{userId}/followers")
    fun follow(
        @PathVariable userId: Long,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<FollowResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(FollowResponse(isFollowing = true, followersCnt = 129))
    }

    /** 언팔로우(모킹). */
    @DeleteMapping("/{userId}/followers/{followerId}")
    fun unfollow(
        @PathVariable userId: Long,
        @PathVariable followerId: Long,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<FollowResponse> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.success(FollowResponse(isFollowing = false, followersCnt = 128))
    }
}
