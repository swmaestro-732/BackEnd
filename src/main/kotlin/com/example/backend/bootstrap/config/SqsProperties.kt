package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * AWS SQS 연결 설정 — 코스 개수 델타 큐. 값은 배포 시 env 로 주입한다.
 * [courseCountQueueUrl] 가 비면(로컬·CI) 클라이언트 빈·폴러를 만들지 않는다(fail-soft) — [SqsConfig]·폴러의 @ConditionalOnExpression.
 * [endpoint] 가 설정되면(LocalStack 등) 엔드포인트를 오버라이드한다.
 */
@ConfigurationProperties(prefix = "aws.sqs")
data class SqsProperties(
    val courseCountQueueUrl: String = "",
    val endpoint: String = "",
    val pollIntervalMs: Long = 1000,
)
