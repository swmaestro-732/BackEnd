package com.example.backend.course.application.port.outbound

/** 조회자의 코스 관점 상태 — 저장 여부·완주(따라가기) 여부. user 도메인 상태를 course 경계 안으로 복사한 DTO. */
data class ViewerCourseState(
    val courseId: Long,
    val hasSaved: Boolean,
    val hasStartedCourse: Boolean,
)

/**
 * 아웃바운드 포트 — 조회자와 코스/작성자 간 상호작용 조회(저장·완주 상태, 팔로우 여부).
 * 이 데이터(saved_courses·tracing_courses·follows)는 user 도메인 소유라, course 애플리케이션은 이 포트로만 접근하고
 * 어댑터가 user 인바운드 포트에 위임한다(크로스 도메인 격리). MSA 분리 시 어댑터만 user 서비스 클라이언트로 교체한다.
 */
interface ViewerInteractionPort {
    /** 여러 코스에 대한 조회자 상태를 코스별로 반환한다(상세·목록 조합용). */
    fun getViewerStates(
        viewerId: Long,
        courseIds: List<Long>,
    ): List<ViewerCourseState>

    /** 조회자(followerId)가 대상(followingId)을 팔로우하는지 — FOLLOWER 공개 코스 열람 권한 판정용. */
    fun isFollowing(
        followerId: Long,
        followingId: Long,
    ): Boolean
}
