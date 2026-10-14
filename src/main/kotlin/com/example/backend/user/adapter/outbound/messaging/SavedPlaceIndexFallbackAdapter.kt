package com.example.backend.user.adapter.outbound.messaging

import com.example.backend.bootstrap.config.SqsProperties
import com.example.backend.user.adapter.messaging.SavedPlaceIndexMessage
import com.example.backend.user.application.port.outbound.SavedPlaceIndexDocument
import com.example.backend.user.application.port.outbound.SavedPlaceIndexFallbackPort
import io.awspring.cloud.sqs.operations.SqsTemplate
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — 동기 색인 실패 시 저장 장소 색인 연산을 폴백 큐(SQS)로 발행한다([SavedPlaceIndexFallbackPort]).
 *
 * [SqsTemplate] 이 없거나(=SQS 미설정, 로컬·CI) queueUrl 이 비면 no-op. 폴백 발행마저 실패하면 색인 반영이
 * 유실될 수 있으므로 error 로그를 남긴다(주 경로도 실패, 재처리 경로도 끊긴 상황 — 재색인 잡이 최종 보정).
 */
@Component
class SavedPlaceIndexFallbackAdapter(
    private val sqsTemplateProvider: ObjectProvider<SqsTemplate>,
    private val sqsProperties: SqsProperties,
) : SavedPlaceIndexFallbackPort {
    private val log = KotlinLogging.logger {}

    override fun enqueueIndex(document: SavedPlaceIndexDocument) =
        send(
            SavedPlaceIndexMessage(
                op = SavedPlaceIndexMessage.OP_INDEX,
                userId = document.userId,
                placeId = document.placeId,
                lat = document.location.latitude,
                lon = document.location.longitude,
                savedAtEpochMs = document.savedAt.toEpochMilli(),
            ),
        )

    override fun enqueueDelete(
        userId: Long,
        placeId: Long,
    ) = send(SavedPlaceIndexMessage(op = SavedPlaceIndexMessage.OP_DELETE, userId = userId, placeId = placeId))

    private fun send(message: SavedPlaceIndexMessage) {
        val template = sqsTemplateProvider.ifAvailable ?: return // SQS 미설정 → no-op
        val queueUrl = sqsProperties.savedPlaceQueueUrl
        if (queueUrl.isBlank()) {
            log.warn { "SQS queueUrl 미설정으로 저장 장소 색인 폴백 발행 생략: ${message.op} ${message.userId}:${message.placeId}" }
            return
        }
        try {
            template.send { to -> to.queue(queueUrl).payload(message) }
        } catch (e: Exception) {
            log.error {
                "저장 장소 색인 폴백 발행 실패(반영 유실 가능): ${message.op} ${message.userId}:${message.placeId} — ${e.message}"
            }
        }
    }
}
