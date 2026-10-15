package com.example.backend.media.adapter.outbound.s3

import com.example.backend.bootstrap.config.MediaProperties
import com.example.backend.media.application.port.outbound.MediaStoragePort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest

@Component
class S3MediaStorageAdapter(
    private val s3Presigner: S3Presigner,
    private val s3Client: S3Client,
    private val mediaProperties: MediaProperties,
) : MediaStoragePort {
    private val log = KotlinLogging.logger {}

    override fun presignedPutUrl(
        key: String,
        contentType: String,
        contentLength: Long,
    ): String {
        val putObjectRequest =
            PutObjectRequest
                .builder()
                .bucket(mediaProperties.bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build()

        val presignRequest =
            PutObjectPresignRequest
                .builder()
                .signatureDuration(mediaProperties.presignTtl)
                .putObjectRequest(putObjectRequest)
                .build()

        return s3Presigner.presignPutObject(presignRequest).url().toString()
    }

    override fun publicUrl(key: String): String = "${mediaProperties.cdnBaseUrl.trimEnd('/')}/$key"

    override fun delete(key: String) {
        try {
            s3Client.deleteObject { it.bucket(mediaProperties.bucket).key(key) }
        } catch (exception: Exception) {
            log.warn(exception) { "S3 객체 삭제 실패(무시): key=$key" }
        }
    }
}
