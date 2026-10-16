package com.example.backend.client.s3

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "aws.s3")
data class S3Properties(
    @field:NotBlank
    val bucket: String,
    @field:NotBlank
    val cdnBaseUrl: String,
    val presignTtl: Duration,
    val endpoint: String = "",
) {
    init {
        require(!presignTtl.isNegative && !presignTtl.isZero) { "presignTtl 은 양수여야 합니다: $presignTtl" }
    }
}
