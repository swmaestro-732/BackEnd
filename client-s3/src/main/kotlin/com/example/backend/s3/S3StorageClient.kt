package com.example.backend.s3

import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

/**
 * S3 객체 작업. 버킷, 키 같은 원시 값만 받는다. 어떤 버킷과 키를 쓸지는 호출하는 어댑터가 정한다.
 * 실패는 예외로 그대로 던진다.
 */
@Component
class S3StorageClient(
    private val s3Presigner: S3Presigner,
    private val s3Client: S3Client,
) {
    /** content-type, content-length 를 서명에 포함한 PUT 프리사인 URL. */
    fun presignPut(
        bucket: String,
        key: String,
        contentType: String,
        contentLength: Long,
        ttl: Duration,
    ): String {
        val putObjectRequest =
            PutObjectRequest
                .builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build()

        val presignRequest =
            PutObjectPresignRequest
                .builder()
                .signatureDuration(ttl)
                .putObjectRequest(putObjectRequest)
                .build()

        return s3Presigner.presignPutObject(presignRequest).url().toString()
    }

    fun deleteObject(
        bucket: String,
        key: String,
    ) {
        s3Client.deleteObject { it.bucket(bucket).key(key) }
    }
}
