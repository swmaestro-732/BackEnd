package com.example.backend.user.adapter.outbound.messaging

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.sqs.SqsMessageSender
import com.example.backend.user.adapter.messaging.CourseCountMessage
import com.example.backend.user.application.port.outbound.CourseCountFallbackPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — 동기 카운트 반영 실패 시 공개범위 전이를 폴백 큐(SQS)로 발행한다([CourseCountFallbackPort]).
 *
 * SqsTemplate 이 없거나(=SQS 미설정, 로컬, CI) queueUrl 이 비면 no-op. 폴백 발행마저 실패하면 error 로그를 남기고
 * 예외를 다시 던진다. 호출한 리스너의 이벤트 저장소 기록이 미완료로 남아 같은 eventId 로 재발행된다.
 */
@Component
class CourseCountFallbackAdapter(
    private val sqsMessageSender: SqsMessageSender,
    private val sqsProperties: SqsProperties,
) : CourseCountFallbackPort {
    private val log = KotlinLogging.logger {}

    override fun publish(
        authorId: Long,
        oldVisibility: CourseVisibility?,
        newVisibility: CourseVisibility?,
        eventId: String,
    ) {
        if (!sqsMessageSender.isAvailable) return // SQS 미설정 → no-op
        val queueUrl = sqsProperties.courseCountQueueUrl
        if (queueUrl.isBlank()) {
            log.warn { "SQS queueUrl 미설정으로 코스 개수 폴백 발행 생략: eventId=$eventId" }
            return
        }
        val message =
            CourseCountMessage(
                authorId = authorId,
                oldVisibility = oldVisibility?.name,
                newVisibility = newVisibility?.name,
                eventId = eventId,
            )
        try {
            sqsMessageSender.send(queueUrl, message)
        } catch (e: Exception) {
            log.error { "코스 개수 폴백 발행 실패(이벤트 재발행 대기): eventId=$eventId, ${e.message}" }
            throw e
        }
    }
}
