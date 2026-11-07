package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.FollowResult
import com.example.backend.user.application.port.inbound.dto.UpdateProfileCommand
import com.example.backend.user.application.port.inbound.dto.UserProfileResult

interface AccountUseCase {
    fun getProfile(userId: Long): UserProfileResult

    fun updateProfile(
        userId: Long,
        command: UpdateProfileCommand,
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
