package com.example.backend.user.application.event

/**
 * 사용자가 탈퇴했다는 사실. user 는 다른 도메인이 무엇을 정리할지 모르고 사실만 알린다.
 * [savedCourseIds] 는 탈퇴 직전 살아 있던 저장 코스 id 다. user 가 저장 레코드를 지운 뒤에는 구독자가 알 수 없어 함께 싣는다.
 */
data class UserWithdrawnEvent(
    val userId: Long,
    val savedCourseIds: List<Long>,
)
