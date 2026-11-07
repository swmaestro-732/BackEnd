package com.example.backend.bootstrap.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.kakao")
data class KakaoOauthProperties(
    // 웹 로그인 id_token 의 aud = REST API 키. (로그인용, 지도 kakao.local.rest-key 와 별개)
    val clientId: String,
    // 안드로이드/iOS SDK 로그인 id_token 의 aud = 네이티브 앱 키. 미설정 시 빈 값(웹만 허용).
    val nativeAppKey: String = "",
    val jwksUri: String,
    val issuer: String,
)
