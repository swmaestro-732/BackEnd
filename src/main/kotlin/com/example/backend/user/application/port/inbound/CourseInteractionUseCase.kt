package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.port.inbound.dto.CourseViewerState

/**
 * 인바운드 포트 — 사용자와 코스 간 상호작용·사용자 간 관계 조회(공개 API).
 *
 * 코스 저장(saved_courses)·따라가기(tracing_courses)·팔로우(follows)는 user 도메인 소유 데이터다.
 * 다른 도메인(course)이 조회자 상태나 작성자와의 관계를 필요로 할 때 이 포트로만 접근한다(크로스 도메인 격리).
 */
interface CourseInteractionUseCase {
    fun getViewerState(
        userId: Long,
        courseId: Long,
    ): CourseViewerState

    /**
     * 조회자(followerId)가 대상 사용자(followingId)를 팔로우하는지.
     * FOLLOWER 공개 코스의 열람 권한 판정에 쓴다.
     */
    fun isFollowing(
        followerId: Long,
        followingId: Long,
    ): Boolean
}
