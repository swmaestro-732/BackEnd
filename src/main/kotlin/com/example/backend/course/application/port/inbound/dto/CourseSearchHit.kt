package com.example.backend.course.application.port.inbound.dto

import java.time.Instant

/**
 * 검색 히트 한 건 — 코스 카드 요약. 인바운드 포트 계약(공개 결과)이라 BFF·다른 도메인도 참조할 수 있게 여기 둔다.
 * 정렬용 내부 지표(score 등)나 커서 표현은 노출하지 않는다 — 커서는 검색 어댑터가 불투명 문자열로만 다룬다.
 */
data class CourseSearchHit(
    val id: Long,
    val authorId: Long,
    val title: String,
    val coverImageUrl: String?,
    val theme: String?,
    val area: String?,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
)
