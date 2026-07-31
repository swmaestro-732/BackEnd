package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * T map 보행자 경로 API 앱 키. 빈 값이면 어댑터가 fail-soft 로 null 을 반환한다.
 */
@ConfigurationProperties(prefix = "tmap")
data class TmapProperties(
    val appKey: String = "",
)
