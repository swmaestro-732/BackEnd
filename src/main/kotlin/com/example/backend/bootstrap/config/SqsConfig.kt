package com.example.backend.bootstrap.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

/**
 * SQS 클라이언트 배선 — `aws.sqs.course-count-queue-url` 가 비어있지 않을 때만 활성(fail-soft).
 * 로컬·CI(큐 미주입)에서는 이 설정 전체가 비활성이라 클라이언트가 생성되지 않고(발행 어댑터는 ObjectProvider 로 no-op),
 * 폴러도 같은 조건으로 비활성이라 앱은 정상 기동한다.
 * 자격증명은 [DefaultCredentialsProvider] — 운영은 EC2 인스턴스 프로파일. `aws.sqs.endpoint` 설정 시 LocalStack 등으로 오버라이드.
 */
@Configuration
@ConditionalOnExpression("'\${aws.sqs.course-count-queue-url:}'.trim().length() > 0")
class SqsConfig(
    private val sqsProperties: SqsProperties,
    @Value("\${aws.region:ap-northeast-2}") private val region: String,
) {
    @Bean
    fun sqsClient(): SqsClient {
        val builder =
            SqsClient
                .builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())

        if (sqsProperties.endpoint.isNotBlank()) {
            builder.endpointOverride(URI.create(sqsProperties.endpoint))
        }

        return builder.build()
    }
}
