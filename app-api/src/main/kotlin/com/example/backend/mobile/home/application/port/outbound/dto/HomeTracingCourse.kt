package com.example.backend.mobile.home.application.port.outbound.dto

import java.time.Instant

/**
 * BFF 아웃바운드 출력 — 진행 중 따라가기 세션과 코스 요약(course 도메인 응답을 BFF 안으로 복사한 격리 DTO).
 * 코스를 더 볼 수 없으면(삭제·비공개 전환) [isDeleted] 가 true 이고 제목·썸네일·지역은 null 이다 — 카드는 남겨 따라가기 화면으로 돌아갈 수 있게.
 */
data class HomeTracingCourse(
    val traceId: Long,
    val courseId: Long,
    val title: String?,
    val coverImageUrl: String?,
    val area: String?,
    val placeCount: Int,
    /** 체크인한 장소 수 — 코스 장소 [placeCount] 중 어디까지 따라갔는지. */
    val visitedPlaceCount: Int,
    val startedAt: Instant,
    val isDeleted: Boolean,
)
