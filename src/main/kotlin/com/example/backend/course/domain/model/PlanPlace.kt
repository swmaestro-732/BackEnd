package com.example.backend.course.domain.model

/** 계획에 담긴 장소([Plan] 애그리거트의 구성 요소). placeId 는 place 도메인 식별자(크로스 도메인, FK 없음). */
data class PlanPlace(
    val placeId: Long,
    val orderNo: Int,
    val memo: String?,
)
