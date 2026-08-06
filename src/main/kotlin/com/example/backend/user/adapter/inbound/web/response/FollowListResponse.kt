package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.FollowListResult

/**
 * 팔로워/팔로잉 목록 조회 응답 — 노션 명세 미등록 신규 API.
 * 사용자 요약(id·닉네임·핸들·프로필 이미지) + 조회자 기준 관계 플래그(isFollowing/isFollower)를 담고,
 * 페이지 메타는 커서 페이지네이션([nextCursor]/[hasNext]) + 전체 개수([totalCount], "팔로워/팔로잉 N" 표기)다.
 */
data class FollowListResponse(
    val totalCount: Int,
    val nextCursor: String?,
    val hasNext: Boolean,
    val users: List<FollowUserResponse>,
) {
    data class FollowUserResponse(
        val id: Long,
        val nickname: String,
        val handle: String?,
        val profileImageUrl: String?,
        val isFollowing: Boolean,
        val isFollower: Boolean,
    )

    companion object {
        /** 인바운드 포트 결과([FollowListResult])를 웹 응답으로 변환한다. */
        fun from(result: FollowListResult): FollowListResponse =
            FollowListResponse(
                totalCount = result.totalCount.toInt(),
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
                users =
                    result.users.map {
                        FollowUserResponse(
                            id = it.id,
                            nickname = it.nickname,
                            handle = it.handle,
                            profileImageUrl = it.profileImageUrl,
                            isFollowing = it.isFollowing,
                            isFollower = it.isFollower,
                        )
                    },
            )

        /** 시드/DB 없이 프론트가 붙어볼 수 있는 고정 목 — 단일 페이지 3명. */
        fun mock(): FollowListResponse {
            val users =
                listOf(
                    FollowUserResponse(
                        id = 2,
                        nickname = "성수러버",
                        handle = "seongsu_lover",
                        profileImageUrl = "https://placehold.co/96x96?text=1",
                        isFollowing = true,
                        isFollower = true,
                    ),
                    FollowUserResponse(
                        id = 3,
                        nickname = "주말산책러",
                        handle = "weekend_walker",
                        profileImageUrl = "https://placehold.co/96x96?text=2",
                        isFollowing = false,
                        isFollower = true,
                    ),
                    FollowUserResponse(
                        id = 4,
                        nickname = "혼걷러",
                        handle = null,
                        profileImageUrl = null,
                        isFollowing = true,
                        isFollower = false,
                    ),
                )
            return FollowListResponse(
                totalCount = users.size,
                nextCursor = null,
                hasNext = false,
                users = users,
            )
        }
    }
}
