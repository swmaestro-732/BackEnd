package com.example.backend.media.adapter.outbound.s3

import com.example.backend.client.s3.S3StorageClient
import com.example.backend.media.application.port.outbound.MediaStoragePort
import org.springframework.stereotype.Component

@Component
class S3MediaStorageAdapter(
    private val s3StorageClient: S3StorageClient,
) : MediaStoragePort {
    override fun presignedPutUrl(
        key: String,
        contentType: String,
        contentLength: Long,
    ): String = s3StorageClient.presignedPutUrl(key, contentType, contentLength)

    override fun publicUrl(key: String): String = s3StorageClient.publicUrl(key)

    override fun keyOf(publicUrl: String): String? = s3StorageClient.keyOf(publicUrl)

    override fun delete(key: String) = s3StorageClient.delete(key)
}
