package com.example.backend.course.application.port.inbound.dto

/** 코스 리뷰 정렬 기준(field). 방향은 [SortDirection] 으로 따로 지정한다. */
enum class CourseReviewSort {
    /** 작성일 기준. */
    LATEST,

    /** 평점 기준(동점은 최신순). */
    RATING,
}

/** 정렬 방향. */
enum class SortDirection {
    ASC,
    DESC,
}

/**
 * 유스케이스 입력 — 코스 리뷰 목록 조회 조건(정렬 기준·방향 + 커서 페이지네이션).
 * cursor 는 직전 응답의 nextCursor 를 그대로 넘기는 불투명 토큰이다(첫 페이지는 null).
 * 잘못된 값은 [IllegalArgumentException] 으로 막아 400 으로 응답한다.
 */
data class CourseReviewQuery(
    val sort: CourseReviewSort = CourseReviewSort.LATEST,
    val order: SortDirection = SortDirection.DESC,
    val cursor: String? = null,
    val size: Int = DEFAULT_SIZE,
) {
    init {
        require(size in 1..MAX_SIZE) { "size 는 1..$MAX_SIZE 범위여야 합니다." }
    }

    companion object {
        const val DEFAULT_SIZE = 10
        const val MAX_SIZE = 50
    }
}
