package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 추천 코스 랭킹 가중치 — OpenSearch function_score 의 항별 weight 와 최신성 감쇠 파라미터(재배포 없이 조정).
 * 점수 = 최신성(exp decay × recencyWeight) + 관심 테마 일치(themeWeight) + 관심 시군구 일치(sigunguWeight)
 *      + 관심 읍면동 일치(dongWeight, 시군구에 가산) + log1p(저장수) × savesWeight + log1p(좋아요수) × likesWeight.
 */
@ConfigurationProperties(prefix = "course.recommend")
data class CourseRecommendProperties(
    val recencyWeight: Double = 10.0,
    /** 감쇠 스케일(OpenSearch 시간 단위 문자열, 예: 14d). 이 시점에 최신성 점수가 recencyWeight × recencyDecay 가 된다. */
    val recencyScale: String = "14d",
    /** 감쇠를 시작하지 않는 구간 — 이 안의 코스는 최신성 만점(동점)이다. */
    val recencyOffset: String = "1d",
    val recencyDecay: Double = 0.5,
    val themeWeight: Double = 4.0,
    val sigunguWeight: Double = 3.0,
    val dongWeight: Double = 2.0,
    val savesWeight: Double = 1.5,
    /** 좋아요는 저장보다 약한 신호라 절반 가중치 — 100개여도 약 3.5점으로 관심 일치 한 항목을 넘지 않는다. */
    val likesWeight: Double = 0.75,
)
