package com.example.backend.bootstrap.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

/**
 * S3 프리사인 URL 발급용 [S3Presigner] 빈.
 * 자격증명은 [DefaultCredentialsProvider] — 운영은 EC2 인스턴스 프로파일, 로컬은 `~/.aws/credentials`.
 * `aws.s3.endpoint` 가 설정되면(LocalStack 등) 엔드포인트 오버라이드 + path-style 접근을 적용한다.
 */
@Configuration
class S3Config(
    private val mediaProperties: MediaProperties,
    @Value("\${aws.region:ap-northeast-2}") private val region: String,
) {
    @Bean
    fun s3Presigner(): S3Presigner {
        val builder =
            S3Presigner
                .builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())

        if (mediaProperties.endpoint.isNotBlank()) {
            builder
                .endpointOverride(URI.create(mediaProperties.endpoint))
                .serviceConfiguration(
                    S3Configuration
                        .builder()
                        .pathStyleAccessEnabled(true)
                        .build(),
                )
        }

        return builder.build()
    }

    /** 객체 삭제(고아 이미지 정리) 등 서버측 S3 작업용 [S3Client]. presigner 와 동일한 자격증명·엔드포인트 정책. */
    @Bean
    fun s3Client(): S3Client {
        val builder =
            S3Client
                .builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())

        if (mediaProperties.endpoint.isNotBlank()) {
            builder
                .endpointOverride(URI.create(mediaProperties.endpoint))
                .serviceConfiguration(
                    S3Configuration
                        .builder()
                        .pathStyleAccessEnabled(true)
                        .build(),
                )
        }

        return builder.build()
    }
}
