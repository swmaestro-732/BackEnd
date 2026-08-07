package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.FollowResult
import com.example.backend.user.application.port.inbound.dto.UserProfileResult

interface AccountUseCase {
    fun getProfile(userId: Long): UserProfileResult

    // 기존 프로필 수정 스타일을 유지해 선택 필드를 개별 파라미터로 받는다.
    fun updateProfile(
        userId: Long,
        nickname: String?,
        handle: String?,
        profileImageUrl: String?,
        bio: String?,
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
