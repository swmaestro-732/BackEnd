package com.example.backend.bootstrap.appversion

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.version")
data class AppVersionProperties(
    val features: Map<String, PlatformMinBuild> = emptyMap(),
) {
    data class PlatformMinBuild(
        val androidVersion: Int? = null,
        val iosVersion: Int? = null,
    )
}
