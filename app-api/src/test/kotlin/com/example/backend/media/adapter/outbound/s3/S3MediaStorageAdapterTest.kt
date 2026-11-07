package com.example.backend.media.adapter.outbound.s3

import com.example.backend.bootstrap.config.MediaProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.time.Duration

class S3MediaStorageAdapterTest {
    // 네트워크 호출 없이 로컬에서 서명만 계산되므로 더미 리전·자격증명으로 충분하다.
    private val s3Presigner =
        S3Presigner
            .builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy")))
            .build()

    // presign 테스트에서는 사용되지 않는다(네트워크 호출 없이 빌드만). delete 는 fail-soft 라 통합 테스트 대상.
    private val s3Client =
        S3Client
            .builder()
            .region(Region.AP_NORTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("dummy", "dummy")))
            .build()

    private val mediaProperties =
        MediaProperties(
            bucket = "dummy-bucket",
            cdnBaseUrl = "https://cdn.example.com",
            presignTtl = Duration.ofMinutes(5),
        )

    private val adapter = S3MediaStorageAdapter(s3Presigner, s3Client, mediaProperties)

    @Test
    fun `presignedPutUrl은 서명 쿼리 파라미터가 포함된 URL을 반환한다`() {
        val url = adapter.presignedPutUrl("profile/1/uuid.jpg", "image/jpeg", 1024)

        assertTrue(url.contains("dummy-bucket"))
        assertTrue(url.contains("profile/1/uuid.jpg"))
        assertTrue(url.contains("X-Amz-Signature"))
    }

    @Test
    fun `presignedPutUrl은 content-length와 content-type을 서명 헤더에 포함한다`() {
        val url = adapter.presignedPutUrl("profile/1/uuid.jpg", "image/jpeg", 1024)

        val signedHeaders =
            Regex("X-Amz-SignedHeaders=([^&]+)")
                .find(url)
                ?.groupValues
                ?.get(1)
                ?.lowercase()
                .orEmpty()

        assertTrue(signedHeaders.contains("content-length"))
        assertTrue(signedHeaders.contains("content-type"))
    }

    @Test
    fun `publicUrl은 cdnBaseUrl과 key를 슬래시 하나로 결합한다`() {
        assertEquals("https://cdn.example.com/profile/1/uuid.jpg", adapter.publicUrl("profile/1/uuid.jpg"))
    }
}
