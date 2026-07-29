package com.example.backend.media.application.service

import com.example.backend.bootstrap.config.MediaProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.media.application.port.inbound.MediaCleanupUseCase
import com.example.backend.media.application.port.inbound.PresignUploadUseCase
import com.example.backend.media.application.port.inbound.dto.PresignCommand
import com.example.backend.media.application.port.inbound.dto.PresignResult
import com.example.backend.media.application.port.outbound.MediaStoragePort
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.UUID

@Service
class MediaService(
    private val mediaStoragePort: MediaStoragePort,
    private val mediaProperties: MediaProperties,
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

        // 트랜잭션 안에서 호출되면(예: 프로필 수정) 커밋 성공 후에만 삭제한다.
        // 롤백 시 DB엔 옛 이미지 URL이 남는데 S3 객체만 지워지는 정합성 붕괴를 막는다.
        // 트랜잭션 밖 호출이면 즉시 삭제. 삭제 자체는 fail-soft(어댑터에서 예외 무시).
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        mediaStoragePort.delete(key)
                    }
                },
            )
        } else {
            mediaStoragePort.delete(key)
        }
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
