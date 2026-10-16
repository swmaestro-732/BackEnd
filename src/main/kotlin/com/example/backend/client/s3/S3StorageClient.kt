package com.example.backend.client.s3

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest

/** S3 버킷 객체의 프리사인 PUT URL 발급·CDN URL 변환·삭제. */
@Component
class S3StorageClient(
    private val s3Presigner: S3Presigner,
    private val s3Client: S3Client,
    private val s3Properties: S3Properties,
) {
    private val log = KotlinLogging.logger {}

    private val cdnPrefix = s3Properties.cdnBaseUrl.trimEnd('/') + "/"

    fun presignedPutUrl(
        key: String,
        contentType: String,
        contentLength: Long,
    ): String {
        val putObjectRequest =
            PutObjectRequest
                .builder()
                .bucket(s3Properties.bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build()

        val presignRequest =
            PutObjectPresignRequest
                .builder()
                .signatureDuration(s3Properties.presignTtl)
                .putObjectRequest(putObjectRequest)
                .build()

        return s3Presigner.presignPutObject(presignRequest).url().toString()
    }

    fun publicUrl(key: String): String = cdnPrefix + key

    /** 우리 CDN URL 이면 객체 키, 아니면(외부·목 URL) null. */
    fun keyOf(publicUrl: String): String? =
        publicUrl
            .takeIf { it.startsWith(cdnPrefix) }
            ?.removePrefix(cdnPrefix)
            ?.takeIf(String::isNotBlank)

    fun delete(key: String) {
        try {
            s3Client.deleteObject { it.bucket(s3Properties.bucket).key(key) }
        } catch (exception: Exception) {
            log.warn(exception) { "S3 객체 삭제 실패(무시): key=$key" }
        }
    }
}
