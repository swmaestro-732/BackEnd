package com.example.backend.user.application.port.inbound.dto

/**
 * 유스케이스 출력 — 사용자 프로필. 코스 상세의 작성자 카드 등 여러 화면에서 재사용한다.
 *
 * isFollowing/isFollower 는 조회자 기준의 관계 데이터다.
 */
data class UserProfileResult(
    val id: Long,
    val nickname: String,
    val handle: String?,
    val profileImageUrl: String?,
    val bio: String?,
    /** 조회자가 이 사용자를 팔로우하는지(내가 → 대상). */
    val isFollowing: Boolean,
    /** 이 사용자가 조회자를 팔로우하는지(대상 → 나, 즉 내 팔로워). */
    val isFollower: Boolean,
    val followersCnt: Int,
    val followingsCnt: Int,
    val coursesCnt: Int,
    /** 공개범위별 발행 코스 개수(저장 캐시). 마이페이지가 조회자 마스킹 후 합산해 노출한다. */
    val publicCoursesCnt: Int,
    val followerCoursesCnt: Int,
    val privateCoursesCnt: Int,
)
