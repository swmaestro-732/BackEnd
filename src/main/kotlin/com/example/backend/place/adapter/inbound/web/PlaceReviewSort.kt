package com.example.backend.place.adapter.inbound.web

/**
 * 장소 리뷰 정렬 기준(field). 방향은 [SortDirection] 으로 따로 지정한다.
 * 디자인(후기 전체보기 상단 "최신순 / 높은 평점" 칩)에서 도출했다.
 */
enum class PlaceReviewSort {
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
