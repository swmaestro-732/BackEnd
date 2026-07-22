package com.example.backend.media.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.media.application.port.inbound.dto.PresignCommand
import com.example.backend.media.application.port.inbound.dto.UploadPurpose
import com.example.backend.media.application.port.outbound.MediaStoragePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaServiceTest {
    private val fakePort =
        object : MediaStoragePort {
            override fun presignedPutUrl(
                key: String,
                contentType: String,
            ): String = "https://bucket.s3.amazonaws.com/$key?presigned"

            override fun publicUrl(key: String): String = "https://cdn.example.com/$key"
        }

    private val service = MediaService(fakePort)

    @Test
    fun `허용된 contentType이면 확장자를 매핑하고 profile 키 형식으로 발급한다`() {
        val result =
            service.presign(PresignCommand(userId = 1L, purpose = UploadPurpose.PROFILE, contentType = "image/jpeg"))

        assertTrue(result.key.matches(Regex("""profile/1/[0-9a-f-]{36}\.jpg""")))
        assertEquals("https://cdn.example.com/${result.key}", result.imageUrl)
        assertEquals("https://bucket.s3.amazonaws.com/${result.key}?presigned", result.uploadUrl)
    }

    @Test
    fun `png와 webp도 허용한다`() {
        val png =
            service.presign(
                PresignCommand(userId = 1L, purpose = UploadPurpose.PROFILE, contentType = "image/png"),
            )
        val webp =
            service.presign(
                PresignCommand(userId = 1L, purpose = UploadPurpose.PROFILE, contentType = "image/webp"),
            )

        assertTrue(png.key.endsWith(".png"))
        assertTrue(webp.key.endsWith(".webp"))
    }

    @Test
    fun `허용되지 않은 contentType이면 UNSUPPORTED_MEDIA_TYPE 예외를 던진다`() {
        val exception =
            assertThrows(BusinessException::class.java) {
                service.presign(
                    PresignCommand(userId = 1L, purpose = UploadPurpose.PROFILE, contentType = "application/pdf"),
                )
            }

        assertEquals(ErrorCode.UNSUPPORTED_MEDIA_TYPE, exception.errorCode)
    }
}
