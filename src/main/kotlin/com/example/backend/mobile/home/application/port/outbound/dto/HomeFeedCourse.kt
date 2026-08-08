package com.example.backend.mobile.home.application.port.outbound.dto

import java.time.Instant

/**
 * BFF 아웃바운드 출력 — 공개 코스 피드 후보. course 도메인 응답([CourseSummary])을 BFF 안으로 복사한 격리 DTO다.
 * theme 은 도메인 enum 이 아니라 이름 문자열(크로스 도메인·BFF 격리).
 * savesCnt 는 course 도메인이 저장/취소 시 갱신하는 denormalized 카운터 값이다.
 */
data class HomeFeedCourse(
    val id: Long,
    val authorId: Long,
    val title: String,
    val coverImageUrl: String?,
    val theme: String?,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Instant,
)

/** BFF 내부 공개 코스 피드 커서. course 도메인의 커서 계약과 어댑터에서 상호 변환한다. */
data class HomeFeedCursor(
    val savesCnt: Int,
    val createdAt: Instant,
    val id: Long,
)

/** BFF 아웃바운드 포트가 반환하는 공개 코스 피드 한 페이지. */
data class HomeFeedCoursePage(
    val courses: List<HomeFeedCourse>,
    val hasNext: Boolean,
)
