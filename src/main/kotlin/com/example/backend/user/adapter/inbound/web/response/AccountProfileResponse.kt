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
    val bio: String?,
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
                bio = result.bio,
                followersCnt = result.followersCnt,
                followingsCnt = result.followingsCnt,
                // 본인 계정 프로필이라 전체 공개범위가 보인다 → 세 버킷 합.
                coursesCnt = result.publicCoursesCnt + result.followerCoursesCnt + result.privateCoursesCnt,
            )

        /**
         * 목 프로필. 수정 목(`?mock=true`)에서 부분 수정 왕복을 흉내내도록, 넘긴 필드는 반영하고
         * null 필드는 고정 목값을 유지한다(부분 수정 의미). 조회 목은 인자 없이 호출해 고정 목을 받는다.
         */
        fun mock(
            nickname: String? = null,
            handle: String? = null,
            profileImageUrl: String? = null,
            bio: String? = null,
        ): AccountProfileResponse =
            AccountProfileResponse(
                id = 1L,
                nickname = nickname ?: "현우님",
                handle = handle ?: "hyunwoo",
                profileImageUrl = profileImageUrl ?: "https://cdn.example.com/users/1.jpg",
                bio = bio ?: "안녕하세요, 커미입니다.",
                followersCnt = 128,
                followingsCnt = 88,
                coursesCnt = 12,
            )
    }
}
