package com.example.backend.user.adapter.inbound.messaging

import com.example.backend.bootstrap.config.SqsProperties
import com.example.backend.user.application.port.inbound.CourseCountMessageUseCase
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import tools.jackson.databind.ObjectMapper

/**
 * 인바운드 어댑터 — 코스 개수 델타 SQS 큐를 폴링해 [CourseCountMessageUseCase] 로 반영한다.
 *
 * 큐가 설정된 배포에서만 활성(@ConditionalOnExpression) — 로컬·CI 는 큐 미설정이라 이 빈이 생성되지 않는다.
 * long-poll(waitTimeSeconds=10) 로 한 번에 최대 10건을 받아 건별로 처리한다. 처리 성공 시에만 삭제하고,
 * 실패하면 삭제하지 않아(가시성 타임아웃 후) 재전송되게 둔다. 반영은 eventId 로 멱등이라 재전송돼도 안전하다.
 */
@Component
@ConditionalOnExpression("'\${aws.sqs.course-count-queue-url:}'.trim().length() > 0")
class CourseCountSqsPoller(
    private val sqsClient: SqsClient,
    private val sqsProperties: SqsProperties,
    private val objectMapper: ObjectMapper,
    private val courseCountMessageUseCase: CourseCountMessageUseCase,
) {
    private val log = KotlinLogging.logger {}

    @Scheduled(fixedDelayString = "\${aws.sqs.poll-interval-ms:1000}")
    fun poll() {
        val queueUrl = sqsProperties.courseCountQueueUrl
        val response =
            sqsClient.receiveMessage { req ->
                req.queueUrl(queueUrl).waitTimeSeconds(10).maxNumberOfMessages(10)
            }
        for (message in response.messages()) {
            try {
                val payload = objectMapper.readValue(message.body(), CourseCountMessagePayload::class.java)
                courseCountMessageUseCase.handle(
                    eventId = payload.eventId,
                    userId = payload.userId,
                    publicDelta = payload.publicDelta,
                    followerDelta = payload.followerDelta,
                    privateDelta = payload.privateDelta,
                )
                // 반영 성공한 메시지만 삭제 — 실패 건은 남겨 재전송받는다(at-least-once + 멱등).
                sqsClient.deleteMessage { req -> req.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) }
            } catch (e: Exception) {
                log.warn { "코스 개수 메시지 처리 실패(삭제 보류·재전송 대기): ${e.message}" }
            }
        }
    }
}

/**
 * SQS 메시지 페이로드 — course 도메인의 이벤트 클래스를 참조하지 않고(크로스 도메인 격리)
 * 필드 이름으로만 맞춘 수신측 전용 DTO. producer 의 CourseCountDeltaEvent 직렬화 형태와 이름이 일치한다.
 */
internal data class CourseCountMessagePayload(
    val userId: Long,
    val publicDelta: Int,
    val followerDelta: Int,
    val privateDelta: Int,
    val eventId: String,
)
