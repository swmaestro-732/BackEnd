package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.FollowResult
import com.example.backend.user.application.port.inbound.dto.UserProfileResult

interface AccountUseCase {
    fun getProfile(userId: Long): UserProfileResult

    // 파라미터 4개 미만이면 커맨드로 감싸지 않고 그대로 받는다(팀 컨벤션).
    fun updateProfile(
        userId: Long,
        nickname: String?,
        handle: String?,
        profileImageUrl: String?,
    ): UserProfileResult

    fun follow(
        followerId: Long,
        targetId: Long,
    ): FollowResult

    fun unfollow(
        followerId: Long,
        targetId: Long,
    ): FollowResult
}
