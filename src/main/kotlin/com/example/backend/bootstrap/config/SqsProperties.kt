package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 폴백 큐(SQS) 설정 — 값은 배포 시 env 로 주입한다.
 * 각 큐 url 이 비면(로컬·CI) 해당 폴백 발행 어댑터는 no-op, 수신 리스너는 @ConditionalOnExpression 으로 생성되지 않는다(fail-soft).
 * 클라이언트·리전·자격증명은 spring-cloud-aws 가 `spring.cloud.aws.*` 로 자동 배선한다.
 * - [courseCountQueueUrl]: 코스 개수 반영 폴백([CourseCountFallbackAdapter]/[CourseCountSqsConsumer]).
 * - [savedPlaceQueueUrl]: 저장 장소 색인 폴백([SavedPlaceIndexFallbackAdapter]/[SavedPlaceIndexSqsConsumer]).
 */
@ConfigurationProperties(prefix = "aws.sqs")
data class SqsProperties(
    val courseCountQueueUrl: String = "",
    val savedPlaceQueueUrl: String = "",
)
