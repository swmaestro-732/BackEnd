package com.example.backend.course.application.port.outbound

/**
 * 코스 지역 필터 — 코스 법정동코드(`area_code`)가 [includePrefixes] 중 하나로 시작하고 [excludePrefixes] 로는 시작하지 않아야 통과한다.
 * 시도(2자리)·행정구를 가진 시(4자리)·시군구(5자리) prefix 를 그대로 쓰며, 광주·전남처럼 한 시도 코드를 나눠야 할 때만 exclude 를 쓴다.
 */
data class CourseRegionFilter(
    val includePrefixes: List<String>,
    val excludePrefixes: List<String> = emptyList(),
)
