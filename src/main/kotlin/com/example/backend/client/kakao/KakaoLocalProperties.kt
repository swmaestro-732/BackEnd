package com.example.backend.client.kakao

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 카카오 로컬(키워드 검색) API REST 키. 빈 값이면 클라이언트가 fail-soft 로 빈 결과를 반환한다.
 */
@ConfigurationProperties(prefix = "kakao.local")
data class KakaoLocalProperties(
    val restKey: String = "",
)
