package com.example.backend.user.adapter.outbound.messaging

import com.example.backend.bootstrap.config.SqsProperties
import com.example.backend.common.domain.CourseVisibility
import com.example.backend.user.adapter.messaging.CourseCountMessage
import com.example.backend.user.application.port.outbound.CourseCountFallbackPort
import io.awspring.cloud.sqs.operations.SqsTemplate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — 동기 카운트 반영 실패 시 공개범위 전이를 폴백 큐(SQS)로 발행한다([CourseCountFallbackPort]).
 *
 * [SqsTemplate] 이 없거나(=SQS 미설정, 로컬·CI) queueUrl 이 비면 no-op. 폴백 발행마저 실패하면 카운트가
 * 유실될 수 있으므로 error 로그를 남긴다(주 경로는 이미 실패했고 재시도 경로도 끊긴 상황 — 보정 잡 대상).
 */
@Component
class CourseCountFallbackAdapter(
    private val sqsTemplateProvider: ObjectProvider<SqsTemplate>,
    private val sqsProperties: SqsProperties,
) : CourseCountFallbackPort {
    private val log = KotlinLogging.logger {}

    override fun publish(
        authorId: Long,
        oldVisibility: CourseVisibility?,
        newVisibility: CourseVisibility?,
        eventId: String,
    ) {
        val template = sqsTemplateProvider.ifAvailable ?: return // SQS 미설정 → no-op
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
            template.send { to -> to.queue(queueUrl).payload(message) }
        } catch (e: Exception) {
            log.error { "코스 개수 폴백 발행 실패(카운트 유실 가능): eventId=$eventId — ${e.message}" }
        }
    }
}
