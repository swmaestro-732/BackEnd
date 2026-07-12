package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.dto.UserProfileResult

/** 웹 응답 DTO — 사용자 프로필. 유스케이스 결과([UserProfileResult])를 직렬화 형태로 변환한다. */
data class UserProfileResponse(
    val id: Long,
    val nickname: String,
    val handle: String,
    val profileImageUrl: String,
    val isFollowing: Boolean,
    val isFollower: Boolean,
) {
    companion object {
        fun from(result: UserProfileResult): UserProfileResponse =
            UserProfileResponse(
                id = result.id,
                nickname = result.nickname,
                handle = result.handle,
                profileImageUrl = result.profileImageUrl,
                isFollowing = result.isFollowing,
                isFollower = result.isFollower,
            )
    }
}
