package com.example.backend.media.adapter.inbound.web.request

import com.example.backend.media.application.port.inbound.dto.PresignCommand
import com.example.backend.media.application.port.inbound.dto.UploadPurpose
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/** 프리사인 업로드 URL 요청 DTO. */
data class PresignRequest(
    @field:NotNull
    val purpose: UploadPurpose?,
    @field:NotBlank
    val contentType: String?,
    @field:NotNull
    @field:Positive
    val contentLength: Long?,
) {
    fun toCommand(userId: Long): PresignCommand =
        PresignCommand(
            userId = userId,
            purpose = purpose!!,
            contentType = contentType!!,
            contentLength = contentLength!!,
        )
}
