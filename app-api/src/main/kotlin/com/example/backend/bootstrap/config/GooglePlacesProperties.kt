package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** Google Places API(New) 키. 빈 값이면 어댑터가 fail-soft 로 null(조회 실패)을 반환한다. */
@ConfigurationProperties(prefix = "google.places")
data class GooglePlacesProperties(
    val apiKey: String = "",
)
