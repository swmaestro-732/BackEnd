package com.example.backend.mobile.course.application.port.outbound.dto

import java.time.Instant

/**
 * BFF 아웃바운드 출력 — 공개 코스 피드 후보. course 도메인 응답([CourseSummary])을 BFF 안으로 복사한 격리 DTO다.
 * theme 은 도메인 enum 이 아니라 이름 문자열(크로스 도메인·BFF 격리).
 * savesCnt 는 후보 조회 시엔 도메인 카운터 값이지만, 서비스가 user 도메인 실제 저장수로 덮어 랭킹에 쓴다.
 */
data class FeedCourse(
    val id: Long,
    val authorId: Long,
    val title: String,
    val coverImageUrl: String?,
    val theme: String?,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
)
