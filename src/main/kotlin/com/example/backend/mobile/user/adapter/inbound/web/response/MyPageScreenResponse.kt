package com.example.backend.mobile.user.adapter.inbound.web.response

import com.example.backend.mobile.user.application.port.inbound.dto.MyPageCourseInfo
import com.example.backend.mobile.user.application.port.inbound.dto.MyPageResult
import com.example.backend.user.application.port.inbound.dto.UserProfileResult
import java.time.Instant

/**
 * 웹 응답 DTO — 마이페이지 화면 조합(BFF). 프로필 + 그 사용자의 공개 코스 카드 목록.
 * 나(`/mypage`)·타인(`/mypage/{handle}`) 공통 형태다 — 팔로우 플래그는 타인 조회에서 의미를 갖고,
 * 내 조회에서는 false 로 내려간다.
 */
data class MyPageScreenResponse(
    val profile: MyPageProfileResponse,
    val courses: List<MyPageCourseResponse>,
) {
    companion object {
        fun from(result: MyPageResult): MyPageScreenResponse =
            MyPageScreenResponse(
                profile = MyPageProfileResponse.from(result.profile),
                courses = result.courses.map(MyPageCourseResponse::from),
            )

        /** `?mock=true` 폴백 — 로그인 사용자 목(id=1 · AccountProfileResponse.mock)과 값을 맞춰 둔다. */
        val MOCK: MyPageScreenResponse =
            MyPageScreenResponse(
                profile =
                    MyPageProfileResponse(
                        id = 1,
                        nickname = "현우님",
                        handle = "hyunwoo",
                        profileImageUrl = "https://cdn.example.com/users/1.jpg",
                        isFollowing = false,
                        isFollower = false,
                        followersCnt = 128,
                        followingsCnt = 88,
                        coursesCnt = 12,
                    ),
                courses =
                    listOf(
                        MyPageCourseResponse(
                            id = "3",
                            title = "한남동 갤러리 하나씩 도장깨기",
                            coverImageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600",
                            theme = "CULTURE",
                            likesCnt = 42,
                            savesCnt = 17,
                            createdAt = Instant.parse("2026-03-10T02:00:00Z"),
                        ),
                    ),
            )
    }
}

/** 마이페이지 프로필 카드 — [UserProfileResult] 형태. 팔로우 플래그는 타인 조회 기준. */
data class MyPageProfileResponse(
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
        fun from(profile: UserProfileResult): MyPageProfileResponse =
            MyPageProfileResponse(
                id = profile.id,
                nickname = profile.nickname,
                handle = profile.handle,
                profileImageUrl = profile.profileImageUrl,
                isFollowing = profile.isFollowing,
                isFollower = profile.isFollower,
                followersCnt = profile.followersCnt,
                followingsCnt = profile.followingsCnt,
                coursesCnt = profile.coursesCnt,
            )
    }
}

/** 마이페이지 코스 카드 — 작성자의 코스 요약([MyPageCourseInfo]). theme 은 카테고리 이름. */
data class MyPageCourseResponse(
    val id: String,
    val title: String,
    val coverImageUrl: String?,
    val theme: String?,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
) {
    companion object {
        fun from(course: MyPageCourseInfo): MyPageCourseResponse =
            MyPageCourseResponse(
                id = course.id.toString(),
                title = course.title,
                coverImageUrl = course.coverImageUrl,
                theme = course.theme,
                likesCnt = course.likesCnt,
                savesCnt = course.savesCnt,
                createdAt = course.createdAt,
            )
    }
}
