package com.example.backend.push.adapter.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 푸시 발송 큐(SQS) 설정. 다른 큐와 같은 `aws.sqs` 접두어를 쓰되 push 가 소유한다.
 * - [pushQueueUrl]: FCM 푸시 발송 큐([com.example.backend.push.adapter.outbound.messaging.PushQueueAdapter]/PushSqsConsumer). 비면 발행 생략, 수신 리스너 미생성.
 */
@ConfigurationProperties(prefix = "aws.sqs")
data class PushQueueProperties(
    val pushQueueUrl: String = "",
)
