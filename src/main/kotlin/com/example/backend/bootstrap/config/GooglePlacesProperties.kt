package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Google Places API(New) 키. 빈 값이면 어댑터가 fail-soft 로 null(조회 실패)을 반환한다.
 * backfillOnStartup 이 true 면 부팅 후 운영시간 미동기화 장소를 백그라운드로 채운다(과금 주의).
 */
@ConfigurationProperties(prefix = "google.places")
data class GooglePlacesProperties(
    val apiKey: String = "",
    val backfillOnStartup: Boolean = false,
)
