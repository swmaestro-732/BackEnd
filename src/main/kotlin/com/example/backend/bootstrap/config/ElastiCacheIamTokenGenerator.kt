package com.example.backend.bootstrap.config

import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.signer.Aws4Signer
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams
import software.amazon.awssdk.http.SdkHttpFullRequest
import software.amazon.awssdk.http.SdkHttpMethod
import software.amazon.awssdk.regions.Region
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Component
class ElastiCacheIamTokenGenerator(
    private val properties: RedisProperties,
) {
    private val credentialsProvider = DefaultCredentialsProvider.create()

    fun generateToken(): String {
        val request =
            SdkHttpFullRequest
                .builder()
                .method(SdkHttpMethod.GET)
                .protocol("http")
                .host(properties.cacheName.lowercase())
                .encodedPath("/")
                .appendRawQueryParameter("Action", "connect")
                .appendRawQueryParameter("User", properties.user)
                .build()
        // IAM token valid 15 min, generated fresh per connection.
        // 서명 시각과 만료 시각을 같은 now 로 고정한다 — 따로 두면 그 사이 초 경계가 넘어가며
        // SDK 가 X-Amz-Expires=899 를 만들어 테스트(=900)와 실사용이 간헐적으로 어긋난다.
        val now = Instant.now()
        val params =
            Aws4PresignerParams
                .builder()
                .awsCredentials(credentialsProvider.resolveCredentials())
                .signingName("elasticache")
                .signingRegion(Region.of(properties.region))
                .signingClockOverride(Clock.fixed(now, ZoneOffset.UTC))
                .expirationTime(now.plusSeconds(900))
                .build()
        return Aws4Signer
            .create()
            .presign(request, params)
            .uri
            .toString()
            .removePrefix("http://")
    }
}
