package com.example.backend.course.domain.model

/** 계획에 담긴 장소([Plan] 애그리거트의 구성 요소). placeId 는 place 도메인 식별자(크로스 도메인, FK 없음). */
data class PlanPlace(
    val placeId: Long,
    val orderNo: Int,
    val memo: String?,
    /**
     * 다음 장소까지 도보 소요 시간(분) — 클라이언트가 계산해 보낸다(plan_places.walking_minutes).
     * 코스([CoursePlace])와 같은 규칙이다: -1 은 도보 이동 불가, null 은 마지막 장소(다음 장소 없음).
     * 소요 시간 합계를 낼 때 -1 은 더하면 안 된다.
     */
    val walkingMinutes: Int? = null,
)
