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
) {
    init {
        require(!presignTtl.isNegative && !presignTtl.isZero) { "presignTtl 은 양수여야 합니다: $presignTtl" }
        require(maxUploadBytes > 0) { "maxUploadBytes 는 양수여야 합니다: $maxUploadBytes" }
    }
}
