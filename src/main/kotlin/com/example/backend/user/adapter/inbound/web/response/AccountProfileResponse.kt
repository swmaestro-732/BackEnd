package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.UserAreaResult
import com.example.backend.user.application.port.inbound.dto.UserLikeTagResult
import com.example.backend.user.application.port.inbound.dto.UserProfileResult

/**
 * 내 계정 프로필 조회(GET /api/v1/users)·수정(PATCH /api/v1/users) 공용 응답 DTO — 프로필 편집 화면이 다루는 필드만 담는다.
 * 조회와 수정이 같은 모양이라 편집 후 재조회 없이 응답만으로 화면을 갱신할 수 있다.
 * 팔로워/팔로잉/코스 개수는 편집 대상이 아니고 프로필 화면(마이페이지 BFF)이 따로 내려주므로 여기 두지 않는다.
 */
data class AccountProfileResponse(
    val id: Long,
    val nickname: String,
    val handle: String?,
    val profileImageUrl: String?,
    val bio: String?,
    val likeTags: List<UserLikeTagResponse>,
    val areas: List<UserAreaResponse>,
) {
    companion object {
        fun from(result: UserProfileResult): AccountProfileResponse =
            AccountProfileResponse(
                id = result.id,
                nickname = result.nickname,
                handle = result.handle,
                profileImageUrl = result.profileImageUrl,
                bio = result.bio,
                likeTags = result.likeTags.map(UserLikeTagResponse::from),
                areas = result.areas.map(UserAreaResponse::from),
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
                likeTags =
                    listOf(
                        UserLikeTagResponse(id = 1L, name = "카페 투어"),
                        UserLikeTagResponse(id = 2L, name = "문화·예술"),
                    ),
                areas = listOf(UserAreaResponse(code = "1168010100", name = "역삼동")),
            )
    }
}

/** 관심 테마 한 건 — id 는 코스 태그(tags) id, name 은 표시 이름. 수정 시 [id] 를 likeTagIds 로 되돌려 보낸다. */
data class UserLikeTagResponse(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(result: UserLikeTagResult): UserLikeTagResponse =
            UserLikeTagResponse(id = result.id, name = result.name)
    }
}

/** 관심 지역 한 건 — code 는 저장된 10자리 법정동코드, name 은 표시 이름(동/시군구). */
data class UserAreaResponse(
    val code: String,
    val name: String,
) {
    companion object {
        fun from(result: UserAreaResult): UserAreaResponse = UserAreaResponse(code = result.code, name = result.name)
    }
}
