package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.UserProfileResult

/**
 * 내 계정 프로필(조회/수정) 응답 DTO. 본인 프로필이므로 팔로우 관계 플래그는 두지 않는다.
 * 카운트 3종(팔로워/팔로잉/코스)은 프로필 화면(마이 프로필)에 표시되는 요소 근거.
 */
data class AccountProfileResponse(
    val id: Long,
    val nickname: String,
    val handle: String?,
    val profileImageUrl: String?,
    val followersCnt: Int,
    val followingsCnt: Int,
    val coursesCnt: Int,
) {
    companion object {
        fun from(result: UserProfileResult): AccountProfileResponse =
            AccountProfileResponse(
                id = result.id,
                nickname = result.nickname,
                handle = result.handle,
                profileImageUrl = result.profileImageUrl,
                followersCnt = result.followersCnt,
                followingsCnt = result.followingsCnt,
                coursesCnt = result.coursesCnt,
            )

        fun mock(): AccountProfileResponse =
            AccountProfileResponse(
                id = 1L,
                nickname = "현우님",
                handle = "@hyunwoo",
                profileImageUrl = "https://cdn.example.com/users/1.jpg",
                followersCnt = 128,
                followingsCnt = 88,
                coursesCnt = 12,
            )
    }
}
