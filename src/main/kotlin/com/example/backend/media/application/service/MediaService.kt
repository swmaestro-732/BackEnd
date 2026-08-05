package com.example.backend.media.application.service

import com.example.backend.bootstrap.config.MediaProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.common.support.runAfterCommit
import com.example.backend.media.application.port.inbound.MediaCleanupUseCase
import com.example.backend.media.application.port.inbound.PresignUploadUseCase
import com.example.backend.media.application.port.inbound.dto.PresignBatchCommand
import com.example.backend.media.application.port.inbound.dto.PresignCommand
import com.example.backend.media.application.port.inbound.dto.PresignResult
import com.example.backend.media.application.port.inbound.dto.UploadPurpose
import com.example.backend.media.application.port.outbound.MediaStoragePort
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MediaService(
    private val mediaStoragePort: MediaStoragePort,
    private val mediaProperties: MediaProperties,
) : PresignUploadUseCase,
    MediaCleanupUseCase {
    override fun presign(command: PresignCommand): PresignResult =
        presignOne(
            userId = command.userId,
            purpose = command.purpose,
            contentType = command.contentType,
            contentLength = command.contentLength,
        )

    override fun presignBatch(command: PresignBatchCommand): List<PresignResult> =
        command.images.map { image ->
            presignOne(
                userId = command.userId,
                purpose = command.purpose,
                contentType = image.contentType,
                contentLength = image.contentLength,
            )
        }

    private fun presignOne(
        userId: Long,
        purpose: UploadPurpose,
        contentType: String,
        contentLength: Long,
    ): PresignResult {
        val ext =
            CONTENT_TYPE_EXTENSIONS[contentType]
                ?: throw BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE)

        if (contentLength !in 1..mediaProperties.maxUploadBytes) {
            throw BusinessException(ErrorCode.PAYLOAD_TOO_LARGE)
        }

        val key = "${purpose.keyPrefix}/$userId/${UUID.randomUUID()}.$ext"
        return PresignResult(
            key = key,
            uploadUrl = mediaStoragePort.presignedPutUrl(key, contentType, contentLength),
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

        // 프로필 수정 등 트랜잭션 안에서 호출되면 커밋 성공 후에만 삭제한다(롤백 시 DB엔 옛 URL이
        // 남는데 S3만 지워지는 정합성 붕괴 방지). 삭제 자체는 fail-soft(어댑터에서 예외 무시).
        runAfterCommit { mediaStoragePort.delete(key) }
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
