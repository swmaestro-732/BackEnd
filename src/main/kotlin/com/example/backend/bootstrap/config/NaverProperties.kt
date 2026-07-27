package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 네이버 검색(지역) API 자격증명. 키가 없으면(빈 값) 어댑터가 fail-soft 로 빈 결과를 반환한다.
 */
@ConfigurationProperties(prefix = "naver")
data class NaverProperties(
    val clientId: String = "",
    val clientSecret: String = "",
)
