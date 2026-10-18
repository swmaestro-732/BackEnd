package com.example.backend.bootstrap.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.apple")
data class AppleOauthProperties(
    // iOS 네이티브 로그인 identityToken 의 aud = 앱 Bundle ID.
    val clientId: String,
    // 웹/안드로이드(Sign in with Apple JS) 로그인 identityToken 의 aud = Services ID. 미설정 시 빈 값(iOS 만 허용).
    val serviceId: String = "",
    val jwksUri: String,
    val issuer: String,
)
