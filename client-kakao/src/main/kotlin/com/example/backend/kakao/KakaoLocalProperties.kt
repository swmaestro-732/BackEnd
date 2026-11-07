package com.example.backend.kakao

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 카카오 로컬(키워드 검색) API REST 키. 빈 값이면 호출이 401 로 실패하고 어댑터가 fail-soft 로 처리한다.
 */
@ConfigurationProperties(prefix = "kakao.local")
data class KakaoLocalProperties(
    val restKey: String = "",
)
