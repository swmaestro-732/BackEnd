package com.example.backend.user.domain.model

import com.example.backend.common.domain.CourseVisibility

/**
 * 작성자의 공개범위별 코스 개수 버킷 델타 — 카운트를 소유한 user 도메인의 순수 규칙.
 *
 * course 도메인이 보낸 공개범위 전이(old→new)를 세 버킷(PUBLIC/FOLLOWER/PRIVATE)의 증감으로 환산한다.
 * "어느 공개범위가 어느 버킷으로 가는지"는 카운트를 소유한 이 도메인이 정한다(course 는 전이만 보낸다).
 * 델타는 교환법칙이 성립(순서 무관)해 재전송·재정렬에도 안전하다 — 중복만 eventId 로 거르면 된다.
 */
data class CourseCountDelta(
    val publicDelta: Int,
    val followerDelta: Int,
    val privateDelta: Int,
) {
    /** 세 버킷 모두 변화 없음(old==new) — 카운트 반영이 불필요하다. */
    fun isNoop(): Boolean = publicDelta == 0 && followerDelta == 0 && privateDelta == 0

    companion object {
        /** 전이(old→new)를 버킷 델타로 환산: 빠진 버킷 −1, 들어온 버킷 +1(같으면 0). */
        fun of(
            old: CourseVisibility?,
            new: CourseVisibility?,
        ): CourseCountDelta {
            fun delta(bucket: CourseVisibility) = (if (new == bucket) 1 else 0) - (if (old == bucket) 1 else 0)
            return CourseCountDelta(
                publicDelta = delta(CourseVisibility.PUBLIC),
                followerDelta = delta(CourseVisibility.FOLLOWER),
                privateDelta = delta(CourseVisibility.PRIVATE),
            )
        }
    }
}
