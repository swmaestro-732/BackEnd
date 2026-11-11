package com.example.backend.mobile.home.application.port.outbound.dto

import java.time.Instant

/**
 * BFF 아웃바운드 출력 — 추천 코스 한 건. course 도메인 응답([CourseSummary])을 BFF 안으로 복사한 격리 DTO다.
 * themes 는 도메인 enum 이 아니라 이름 문자열 목록(크로스 도메인·BFF 격리).
 * savesCnt 는 course 도메인이 저장/취소 시 갱신하는 denormalized 카운터 값이다.
 * placeCount 는 코스 장소 수, walkingMinutes 는 구간 도보 시간 합(분)이다.
 */
data class HomeFeedCourse(
    val id: Long,
    val authorId: Long,
    val title: String,
    val coverImageUrl: String?,
    val themes: List<String>,
    val area: String?,
    val placeCount: Int,
    val walkingMinutes: Int,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
)

/** BFF 아웃바운드 포트가 반환하는 추천 코스 한 페이지. [nextCursor] 는 course 도메인이 만든 불투명 커서(다음 페이지 없으면 null). */
data class HomeFeedCoursePage(
    val courses: List<HomeFeedCourse>,
    val hasNext: Boolean,
    val nextCursor: String?,
)
