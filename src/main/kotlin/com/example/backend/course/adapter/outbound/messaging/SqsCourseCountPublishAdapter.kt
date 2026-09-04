package com.example.backend.course.adapter.outbound.messaging

import com.example.backend.bootstrap.config.SqsProperties
import com.example.backend.course.application.event.CourseCountDeltaEvent
import com.example.backend.course.application.port.outbound.CourseCountMessagePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import tools.jackson.databind.ObjectMapper

/**
 * 아웃바운드 어댑터 — [CourseCountMessagePort] 를 SQS 전송으로 구현한다([OpenSearchCourseIndexAdapter] 와 동일한 fail-soft).
 *
 * [SqsClient] 가 없거나(=큐 미설정, 로컬·CI) queueUrl 이 비면 no-op, 전송 예외는 warn 로그만 남기고 삼킨다 —
 * 개수 캐시는 부가 기능이라 코스 쓰기 흐름을 막지 않는다(다음 변경 때 함께 반영되거나 보정 잡이 메운다).
 */
@Component
class SqsCourseCountPublishAdapter(
    private val clientProvider: ObjectProvider<SqsClient>,
    private val sqsProperties: SqsProperties,
    private val objectMapper: ObjectMapper,
) : CourseCountMessagePort {
    private val log = KotlinLogging.logger {}

    override fun send(event: CourseCountDeltaEvent) {
        val client = clientProvider.ifAvailable ?: return // 큐 미설정 → no-op
        val queueUrl = sqsProperties.courseCountQueueUrl
        if (queueUrl.isBlank()) {
            log.warn { "SQS queueUrl 미설정으로 코스 개수 델타 발행 생략: eventId=${event.eventId}" }
            return
        }
        try {
            val body = objectMapper.writeValueAsString(event)
            client.sendMessage { req -> req.queueUrl(queueUrl).messageBody(body) }
        } catch (e: Exception) {
            log.warn { "코스 개수 델타 발행 실패(무시): eventId=${event.eventId} — ${e.message}" }
        }
    }
}
