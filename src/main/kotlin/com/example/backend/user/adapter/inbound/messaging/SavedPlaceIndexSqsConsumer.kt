package com.example.backend.user.adapter.inbound.messaging

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.adapter.messaging.SavedPlaceIndexMessage
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceSearchIndexPort
import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 인바운드 어댑터 — 폴백 큐(SQS)의 저장 장소 색인 메시지를 수신해 [SavedPlaceSearchIndexPort] 로 재처리한다(동기 색인 실패분 재시도).
 *
 * 큐가 설정된 배포에서만 활성(@ConditionalOnExpression) — 로컬·CI 는 큐 미설정이라 이 빈이 생성되지 않는다.
 * 반영 성공 시 spring-cloud-aws 가 자동 ack, 예외면 ack 하지 않아 (가시성 타임아웃 후) 재전송된다.
 * upsert/delete 가 멱등이라 at-least-once 재전송에도 안전하다(CourseCountSqsConsumer 선례).
 */
@Component
@ConditionalOnExpression("'\${aws.sqs.saved-place-queue-url:}'.trim().length() > 0")
class SavedPlaceIndexSqsConsumer(
    private val port: SavedPlaceSearchIndexPort,
) {
    private val log = KotlinLogging.logger {}

    @SqsListener("\${aws.sqs.saved-place-queue-url}")
    fun onMessage(message: SavedPlaceIndexMessage) {
        when (message.op) {
            SavedPlaceIndexMessage.OP_INDEX -> {
                port.index(
                    SavedPlaceIndexDocument(
                        userId = message.userId,
                        placeId = message.placeId,
                        location =
                            Coordinate(
                                latitude = requireNotNull(message.lat),
                                longitude = requireNotNull(message.lon),
                            ),
                        savedAt = Instant.ofEpochMilli(requireNotNull(message.savedAtEpochMs)),
                    ),
                )
            }

            SavedPlaceIndexMessage.OP_DELETE -> {
                port.delete(message.userId, message.placeId)
            }

            else -> {
                log.warn { "알 수 없는 저장 장소 색인 연산 수신(무시): ${message.op}" }
            }
        }
    }
}
