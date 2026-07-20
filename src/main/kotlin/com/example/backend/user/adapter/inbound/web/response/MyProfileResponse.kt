package com.example.backend.user.adapter.inbound.web.response

/**
 * 모킹 응답 DTO — 프로필(조회/수정). 기본 프로필 + 팔로우 관계.
 * 카운트 3종(팔로워/팔로잉/코스)은 프로필 화면(마이 프로필)에 표시되는 요소 근거.
 */
data class MyProfileResponse(
    val id: Long,
    val nickname: String,
    val handle: String,
    val profileImageUrl: String?,
    val isFollowing: Boolean,
    val isFollower: Boolean,
    val followersCnt: Int,
    val followingsCnt: Int,
    val coursesCnt: Int,
)
