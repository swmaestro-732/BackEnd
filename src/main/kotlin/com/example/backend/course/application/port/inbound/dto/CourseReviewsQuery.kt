package com.example.backend.course.application.port.inbound.dto

/**
 * 인바운드 포트 입력 DTO — 코스 리뷰 목록 조회.
 *
 * - [courseId] 리뷰를 읽을 코스.
 * - [sort] 정렬 기준([CourseReviewSortKey]). 화면의 "최신순 / 높은 평점" 칩에 대응한다.
 * - [descending] 내림차순 여부(기본 true — 최신·높은 평점이 위).
 * - [cursor] 직전 응답의 nextCursor(첫 페이지는 null). 형식이 잘못되면 400.
 * - [size] 페이지 크기. 범위 검증은 웹 어댑터가 한다.
 */
data class CourseReviewsQuery(
    val courseId: Long,
    val sort: CourseReviewSortKey = CourseReviewSortKey.LATEST,
    val descending: Boolean = true,
    val cursor: String? = null,
    val size: Int = 10,
)

/**
 * 코스 리뷰 정렬 기준. 어느 정렬이든 (기준값, 작성일, id) 순서로 완전히 정렬돼 커서가 흔들리지 않는다.
 * BFF 화면 파라미터가 그대로 쓰지만, 도메인 공개 계약이라 포트 패키지에 둔다.
 */
enum class CourseReviewSortKey {
    /** 작성일 기준. */
    LATEST,

    /** 평점 기준(동점은 작성일). */
    RATING,
}
