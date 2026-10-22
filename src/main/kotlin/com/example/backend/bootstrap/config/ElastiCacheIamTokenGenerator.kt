package com.example.backend.bootstrap.config

import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.signer.Aws4Signer
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams
import software.amazon.awssdk.http.SdkHttpFullRequest
import software.amazon.awssdk.http.SdkHttpMethod
import software.amazon.awssdk.regions.Region
import java.time.Instant

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
        val params =
            Aws4PresignerParams
                .builder()
                .awsCredentials(credentialsProvider.resolveCredentials())
                .signingName("elasticache")
                .signingRegion(Region.of(properties.region))
                .expirationTime(Instant.now().plusSeconds(900))
                .build()
        return Aws4Signer
            .create()
            .presign(request, params)
            .uri
            .toString()
            .removePrefix("http://")
    }
}
