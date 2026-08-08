package com.example.backend.user.application.port.inbound

/**
 * 인바운드 포트 — 작성자의 공개범위별 코스 개수 캐시 유지. course 도메인이 코스 발행/공개범위변경/삭제 시 호출한다.
 * 마이페이지는 매 조회 GROUP BY 대신 이 캐시(users 컬럼)를 읽는다.
 *
 * 크로스 도메인 경계라 course 의 공개범위 enum 을 넘기지 않고 버킷별 델타(원시 int)만 받는다 — 호출부가 전이를 계산한다.
 */
interface UserCourseCountUseCase {
    /**
     * 세 버킷의 델타를 원자적으로 반영한다. 예: PUBLIC→PRIVATE 변경이면 publicDelta=-1, privateDelta=+1.
     * 모두 0 이면 아무 것도 하지 않는다.
     */
    fun applyCourseCountDelta(
        userId: Long,
        publicDelta: Int,
        followerDelta: Int,
        privateDelta: Int,
    )
}
