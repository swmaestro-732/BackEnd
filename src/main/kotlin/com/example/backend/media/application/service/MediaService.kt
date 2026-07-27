package com.example.backend.media.application.service

import com.example.backend.bootstrap.config.MediaProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.media.application.port.inbound.PresignUploadUseCase
import com.example.backend.media.application.port.inbound.dto.PresignCommand
import com.example.backend.media.application.port.inbound.dto.PresignResult
import com.example.backend.media.application.port.outbound.MediaStoragePort
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MediaService(
    private val mediaStoragePort: MediaStoragePort,
    private val mediaProperties: MediaProperties,
) : PresignUploadUseCase {
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

    private companion object {
        val CONTENT_TYPE_EXTENSIONS =
            mapOf(
                "image/jpeg" to "jpg",
                "image/png" to "png",
                "image/webp" to "webp",
            )
    }
}
