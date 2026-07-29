package com.example.backend.media.application.service

import com.example.backend.bootstrap.config.MediaProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.media.application.event.OrphanMediaDeletionRequested
import com.example.backend.media.application.port.inbound.MediaCleanupUseCase
import com.example.backend.media.application.port.inbound.PresignUploadUseCase
import com.example.backend.media.application.port.inbound.dto.PresignCommand
import com.example.backend.media.application.port.inbound.dto.PresignResult
import com.example.backend.media.application.port.outbound.MediaStoragePort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MediaService(
    private val mediaStoragePort: MediaStoragePort,
    private val mediaProperties: MediaProperties,
    private val eventPublisher: ApplicationEventPublisher,
) : PresignUploadUseCase,
    MediaCleanupUseCase {
    override fun presign(command: PresignCommand): PresignResult {
        val ext =
            CONTENT_TYPE_EXTENSIONS[command.contentType]
                ?: throw BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE)

        if (command.contentLength !in 1..mediaProperties.maxUploadBytes) {
            throw BusinessException(ErrorCode.PAYLOAD_TOO_LARGE)
        }

        val key = "${command.purpose.keyPrefix}/${command.userId}/${UUID.randomUUID()}.$ext"
        return PresignResult(
            key = key,
            uploadUrl = mediaStoragePort.presignedPutUrl(key, command.contentType, command.contentLength),
            imageUrl = mediaStoragePort.publicUrl(key),
        )
    }

    override fun deleteByUrl(imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) return
        // 우리 CDN(cdnBaseUrl) 이 발급한 URL 만 삭제 대상 — 외부/목 URL 은 무시한다.
        val prefix = mediaProperties.cdnBaseUrl.trimEnd('/') + "/"
        if (!imageUrl.startsWith(prefix)) return
        val key = imageUrl.removePrefix(prefix)
        if (key.isBlank()) return

        // 실제 삭제는 커밋 이후로 미룬다 — 이벤트 발행만 하고, 리스너가 AFTER_COMMIT 에 S3 를 삭제한다
        // (롤백 시 DB엔 옛 URL이 남는데 S3만 지워지는 정합성 붕괴 방지).
        eventPublisher.publishEvent(OrphanMediaDeletionRequested(key))
    }

    private companion object {
        val CONTENT_TYPE_EXTENSIONS =
            mapOf(
                "image/jpeg" to "jpg",
                "image/png" to "png",
                "image/webp" to "webp",
            )
    }
}
