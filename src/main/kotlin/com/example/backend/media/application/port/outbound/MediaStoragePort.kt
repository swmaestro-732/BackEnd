package com.example.backend.media.application.port.outbound

interface MediaStoragePort {
    /** 주어진 키·Content-Type·Content-Length 로 S3 PUT 프리사인 URL을 발급한다. */
    fun presignedPutUrl(
        key: String,
        contentType: String,
        contentLength: Long,
    ): String

    /** 업로드 완료 후 클라이언트가 사용할 공개(CDN) URL. */
    fun publicUrl(key: String): String
}
