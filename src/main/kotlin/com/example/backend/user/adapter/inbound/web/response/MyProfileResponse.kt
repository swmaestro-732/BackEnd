package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.UserProfileResult

/**
 * 프로필(조회/수정) 응답 DTO. 기본 프로필 + 팔로우 관계.
 * 카운트 3종(팔로워/팔로잉/코스)은 프로필 화면(마이 프로필)에 표시되는 요소 근거.
 */
data class MyProfileResponse(
    val id: Long,
    val nickname: String,
    val handle: String?,
    val profileImageUrl: String?,
    val isFollowing: Boolean,
    val isFollower: Boolean,
    val followersCnt: Int,
    val followingsCnt: Int,
    val coursesCnt: Int,
) {
    companion object {
        fun from(result: UserProfileResult): MyProfileResponse =
            MyProfileResponse(
                id = result.id,
                nickname = result.nickname,
                handle = result.handle,
                profileImageUrl = result.profileImageUrl,
                isFollowing = result.isFollowing,
                isFollower = result.isFollower,
                followersCnt = result.followersCnt,
                followingsCnt = result.followingsCnt,
                coursesCnt = result.coursesCnt,
            )

        fun mock(): MyProfileResponse =
            MyProfileResponse(
                id = 1L,
                nickname = "현우님",
                handle = "@hyunwoo",
                profileImageUrl = "https://cdn.example.com/users/1.jpg",
                isFollowing = false,
                isFollower = false,
                followersCnt = 128,
                followingsCnt = 88,
                coursesCnt = 12,
            )
    }
}
