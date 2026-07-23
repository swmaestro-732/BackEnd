package com.example.backend.course.adapter.inbound.web

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
