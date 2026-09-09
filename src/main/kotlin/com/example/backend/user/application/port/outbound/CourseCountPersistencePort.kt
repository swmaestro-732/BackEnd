package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — 작성자의 공개범위별 코스 개수 캐시(users 컬럼)를 증감한다.
 * 코스 개수 델타 메시지 처리기([com.example.backend.user.application.service.CourseCountMessageHandler])가
 * 공개범위 전이로부터 계산한 버킷 델타를 반영한다. dedup 은 별도([ProcessedCourseCountEventPort]).
 *
 * 카운트 유지 전용 좁은 포트라 넓은 [UserPersistencePort] 를 끌어오지 않는다(ISP).
 */
interface CourseCountPersistencePort {
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
