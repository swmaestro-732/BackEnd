package com.example.backend.user.application.port.inbound.dto

data class FollowResult(
    val isFollowing: Boolean,
    val followersCnt: Int,
)
