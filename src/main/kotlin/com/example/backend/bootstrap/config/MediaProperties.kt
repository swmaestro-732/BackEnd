package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "aws.s3")
data class MediaProperties(
    val bucket: String,
    val cdnBaseUrl: String,
    val presignTtl: Duration,
    val endpoint: String = "",
)
