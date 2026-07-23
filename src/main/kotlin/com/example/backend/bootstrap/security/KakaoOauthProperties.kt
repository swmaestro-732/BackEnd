package com.example.backend.bootstrap.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oauth.kakao")
data class KakaoOauthProperties(
    val clientId: String,
    val jwksUri: String,
    val issuer: String,
)
