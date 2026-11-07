package com.example.backend.media.adapter.outbound.s3

import com.example.backend.media.application.port.outbound.MediaStoragePort
import com.example.backend.s3.S3StorageClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class S3MediaStorageAdapter(
    private val s3StorageClient: S3StorageClient,
    private val mediaProperties: MediaProperties,
) : MediaStoragePort {
    private val log = KotlinLogging.logger {}

    override fun presignedPutUrl(
        key: String,
        contentType: String,
        contentLength: Long,
    ): String =
        s3StorageClient.presignPut(
            bucket = mediaProperties.bucket,
            key = key,
            contentType = contentType,
            contentLength = contentLength,
            ttl = mediaProperties.presignTtl,
        )

    override fun publicUrl(key: String): String = "${mediaProperties.cdnBaseUrl.trimEnd('/')}/$key"

    override fun delete(key: String) {
        try {
            s3StorageClient.deleteObject(mediaProperties.bucket, key)
        } catch (exception: Exception) {
            log.warn(exception) { "S3 객체 삭제 실패(무시): key=$key" }
        }
    }
}
