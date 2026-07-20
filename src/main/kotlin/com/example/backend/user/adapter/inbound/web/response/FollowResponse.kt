package com.example.backend.user.adapter.inbound.web.response

/** 모킹 응답 DTO — 팔로우/언팔로우 후 상태 + 대상 사용자의 팔로워 수. */
data class FollowResponse(
    val isFollowing: Boolean,
    val followersCnt: Int,
)
