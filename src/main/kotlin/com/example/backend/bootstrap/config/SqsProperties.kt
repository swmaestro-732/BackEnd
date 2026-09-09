package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 코스 개수 폴백 큐(SQS) 설정 — 값은 배포 시 env 로 주입한다.
 * [courseCountQueueUrl] 가 비면(로컬·CI) 폴백 발행([CourseCountFallbackAdapter])은 no-op,
 * 수신 리스너([CourseCountSqsConsumer])는 @ConditionalOnExpression 으로 생성되지 않는다(fail-soft).
 * 클라이언트·리전·자격증명은 spring-cloud-aws 가 `spring.cloud.aws.*` 로 자동 배선한다.
 */
@ConfigurationProperties(prefix = "aws.sqs")
data class SqsProperties(
    val courseCountQueueUrl: String = "",
)
