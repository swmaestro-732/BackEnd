package com.example.backend.mobile.user.application.port.outbound.dto

/**
 * BFF 아웃바운드 출력 — 프로필 스냅샷. user 도메인 응답을 BFF 안으로 복사한 격리 DTO다.
 * MSA 분리 후 프로필 서비스가 어떤 형태로 내려주든 BFF 조합 코드는 이 타입만 안다
 * ([com.example.backend.mobile.user.application.port.outbound.MyPageProfilePort]).
 */
data class ProfileSnapshot(
    val id: Long,
    val nickname: String,
    val handle: String?,
    val profileImageUrl: String?,
    val bio: String?,
    /** 조회자가 이 사용자를 팔로우하는지(내가 → 대상). */
    val isFollowing: Boolean,
    /** 이 사용자가 조회자를 팔로우하는지(대상 → 나). */
    val isFollower: Boolean,
    val followersCnt: Int,
    val followingsCnt: Int,
    /** 공개범위별 발행 코스 개수(저장 캐시). 화면 조합에서 조회자 마스킹 후 합산한다. */
    val publicCoursesCnt: Int,
    val followerCoursesCnt: Int,
    val privateCoursesCnt: Int,
    /** 관심 지역 — 내 마이페이지에서만 값이 있고, 타인 조회는 빈 리스트다(개인 설정 비노출). */
    val areas: List<AreaSnapshot>,
)

/** 관심 지역 한 건 — code 는 10자리 법정동코드, name 은 표시 이름(동/시군구). */
data class AreaSnapshot(
    val code: String,
    val name: String,
)
