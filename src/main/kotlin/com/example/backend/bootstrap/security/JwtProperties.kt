package com.example.backend.bootstrap.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessTtl: Duration,
    val refreshTtl: Duration,
    val registrationTtl: Duration,
)
