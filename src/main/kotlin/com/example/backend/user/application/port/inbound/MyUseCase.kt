package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.FollowResult
import com.example.backend.user.application.port.inbound.dto.UpdateProfileCommand
import com.example.backend.user.application.port.inbound.dto.UserProfileResult

interface MyUseCase {
    fun getMyProfile(userId: Long): UserProfileResult

    fun updateMyProfile(
        userId: Long,
        command: UpdateProfileCommand,
    ): UserProfileResult

    fun withdraw(userId: Long)

    fun follow(
        followerId: Long,
        targetId: Long,
    ): FollowResult

    fun unfollow(
        followerId: Long,
        targetId: Long,
    ): FollowResult
}
