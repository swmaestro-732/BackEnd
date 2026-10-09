package com.example.backend.mobile.home.application.port.inbound.dto

import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCourse

/**
 * 코스 피드 화면 조합 결과 (BFF) — 저장수 내림차순·최신순으로 랭킹된 공개 코스 한 페이지와 커서 메타데이터.
 * 조합은 BFF 자신의 아웃바운드 포트로만 이뤄지며, 재료는 도메인 타입이 아닌 BFF 격리 DTO([HomeFeedCourse])다.
 */
data class HomeFeedResult(
    val courses: List<HomeFeedCourse>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
