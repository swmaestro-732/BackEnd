package com.example.backend.media.application.service

import com.example.backend.bootstrap.config.MediaProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.media.application.port.inbound.dto.PresignCommand
import com.example.backend.media.application.port.inbound.dto.UploadPurpose
import com.example.backend.media.application.port.outbound.MediaStoragePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class MediaServiceTest {
    private val deletedKeys = mutableListOf<String>()

    private val fakePort =
        object : MediaStoragePort {
            override fun presignedPutUrl(
                key: String,
                contentType: String,
                contentLength: Long,
            ): String = "https://bucket.s3.amazonaws.com/$key?presigned"

            override fun publicUrl(key: String): String = "https://cdn.example.com/$key"

            override fun delete(key: String) {
                deletedKeys.add(key)
            }
        }

    private val mediaProperties =
        MediaProperties(
            bucket = "dummy-bucket",
            cdnBaseUrl = "https://cdn.example.com",
            presignTtl = Duration.ofMinutes(5),
            maxUploadBytes = 10_485_760,
        )

    private val service = MediaService(fakePort, mediaProperties)

    @Test
    fun `허용된 contentType이면 확장자를 매핑하고 profile 키 형식으로 발급한다`() {
        val result =
            service.presign(
                PresignCommand(
                    userId = 1L,
                    purpose = UploadPurpose.PROFILE,
                    contentType = "image/jpeg",
                    contentLength = 1024,
                ),
            )

        assertTrue(result.key.matches(Regex("""profile/1/[0-9a-f-]{36}\.jpg""")))
        assertEquals("https://cdn.example.com/${result.key}", result.imageUrl)
        assertEquals("https://bucket.s3.amazonaws.com/${result.key}?presigned", result.uploadUrl)
    }

    @Test
    fun `png와 webp도 허용한다`() {
        val png =
            service.presign(
                PresignCommand(
                    userId = 1L,
                    purpose = UploadPurpose.PROFILE,
                    contentType = "image/png",
                    contentLength = 1024,
                ),
            )
        val webp =
            service.presign(
                PresignCommand(
                    userId = 1L,
                    purpose = UploadPurpose.PROFILE,
                    contentType = "image/webp",
                    contentLength = 1024,
                ),
            )

        assertTrue(png.key.endsWith(".png"))
        assertTrue(webp.key.endsWith(".webp"))
    }

    @Test
    fun `허용되지 않은 contentType이면 UNSUPPORTED_MEDIA_TYPE 예외를 던진다`() {
        val exception =
            assertThrows(BusinessException::class.java) {
                service.presign(
                    PresignCommand(
                        userId = 1L,
                        purpose = UploadPurpose.PROFILE,
                        contentType = "application/pdf",
                        contentLength = 1024,
                    ),
                )
            }

        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, exception.errorCode)
    }

    @Test
    fun `contentLength가 최대치를 초과하면 PAYLOAD_TOO_LARGE 예외를 던진다`() {
        val exception =
            assertThrows(BusinessException::class.java) {
                service.presign(
                    PresignCommand(
                        userId = 1L,
                        purpose = UploadPurpose.PROFILE,
                        contentType = "image/jpeg",
                        contentLength = mediaProperties.maxUploadBytes + 1,
                    ),
                )
            }

        assertEquals(ErrorCode.PAYLOAD_TOO_LARGE, exception.errorCode)
    }

    @Test
    fun `contentLength가 0 이하이면 PAYLOAD_TOO_LARGE 예외를 던진다`() {
        val exception =
            assertThrows(BusinessException::class.java) {
                service.presign(
                    PresignCommand(
                        userId = 1L,
                        purpose = UploadPurpose.PROFILE,
                        contentType = "image/jpeg",
                        contentLength = 0,
                    ),
                )
            }

        assertEquals(ErrorCode.PAYLOAD_TOO_LARGE, exception.errorCode)
    }

    @Test
    fun `deleteByUrl은 우리 CDN URL에서 key만 추출해 삭제한다`() {
        service.deleteByUrl("https://cdn.example.com/profile/1/uuid.jpg")

        assertEquals(listOf("profile/1/uuid.jpg"), deletedKeys)
    }

    @Test
    fun `deleteByUrl은 null·빈값·외부 URL이면 삭제하지 않는다`() {
        service.deleteByUrl(null)
        service.deleteByUrl("   ")
        service.deleteByUrl("https://other.cdn.com/profile/1/uuid.jpg")

        assertTrue(deletedKeys.isEmpty())
    }
}
