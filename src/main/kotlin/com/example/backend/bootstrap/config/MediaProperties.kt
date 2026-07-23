package com.example.backend.bootstrap.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "aws.s3")
data class MediaProperties(
    @field:NotBlank
    val bucket: String,
    @field:NotBlank
    val cdnBaseUrl: String,
    val presignTtl: Duration,
    val endpoint: String = "",
    val maxUploadBytes: Long = 10_485_760,
)
