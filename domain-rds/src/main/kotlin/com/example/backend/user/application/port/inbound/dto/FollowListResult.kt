package com.example.backend.user.application.port.inbound.dto

/**
 * 팔로워/팔로잉 목록 조회 명령 — 인바운드 포트([com.example.backend.user.application.port.inbound.FollowQueryUseCase]) 입력.
 *
 * - targetUserId: 목록의 주인(경로 변수 {userId}). 본인일 수도, 타인일 수도 있다.
 * - viewerId: 조회자(JWT subject, 비로그인이면 null). 각 항목의 isFollowing/isFollower 배선 기준.
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 null). follows 레코드 id 기반 불투명 커서.
 * - size: 페이지 크기(1~50). 웹 어댑터에서 검증한다.
 */
data class FollowListCommand(
    val targetUserId: Long,
    val viewerId: Long?,
    val cursor: String?,
    val size: Int,
)

/**
 * 팔로워/팔로잉 목록 조회 결과 — 사용자 요약 + 조회자 기준 관계 플래그 + 커서 페이지 메타.
 * totalCount 는 대상 사용자의 캐시 카운터(users.followers_cnt / followings_cnt)를 그대로 쓴다.
 */
data class FollowListResult(
    val totalCount: Long,
    val nextCursor: String?,
    val hasNext: Boolean,
    val users: List<FollowUserItem>,
) {
    data class FollowUserItem(
        val id: Long,
        val nickname: String,
        val handle: String?,
        val profileImageUrl: String?,
        // 조회자(viewer)가 이 사용자를 팔로우하는가. 비로그인이면 false.
        val isFollowing: Boolean,
        // 이 사용자가 조회자(viewer)를 팔로우하는가. 비로그인이면 false.
        val isFollower: Boolean,
    )
}
