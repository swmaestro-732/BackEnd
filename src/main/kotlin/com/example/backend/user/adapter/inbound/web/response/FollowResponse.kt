package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.FollowResult

/** 팔로우/언팔로우 후 상태 + 대상 사용자의 팔로워 수. */
data class FollowResponse(
    val isFollowing: Boolean,
    val followersCnt: Int,
) {
    companion object {
        fun from(result: FollowResult): FollowResponse =
            FollowResponse(
                isFollowing = result.isFollowing,
                followersCnt = result.followersCnt,
            )

        fun mock(isFollowing: Boolean): FollowResponse =
            FollowResponse(isFollowing = isFollowing, followersCnt = if (isFollowing) 129 else 128)
    }
}
