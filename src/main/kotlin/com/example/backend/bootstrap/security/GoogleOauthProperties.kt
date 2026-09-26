package com.example.backend.bootstrap.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.google")
data class GoogleOauthProperties(
    // 웹 로그인 id_token 의 aud = GCP OAuth 웹 client-id.
    val clientId: String,
    // 안드로이드 SDK 로그인 id_token 의 aud = 안드로이드 client-id. 미설정 시 빈 값(웹만 허용).
    val androidClientId: String = "",
    // iOS SDK 로그인 id_token 의 aud = iOS client-id. 미설정 시 빈 값.
    val iosClientId: String = "",
    val jwksUri: String,
    val issuer: String,
)
