package com.example.backend.media.adapter.inbound.web.request

import com.example.backend.media.application.port.inbound.dto.PresignBatchCommand
import com.example.backend.media.application.port.inbound.dto.PresignItem
import com.example.backend.media.application.port.inbound.dto.UploadPurpose
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/** 여러 이미지의 프리사인 업로드 URL 요청 DTO. */
data class PresignBatchRequest(
    @field:NotNull
    val purpose: UploadPurpose?,
    @field:NotEmpty
    @field:Size(max = 10)
    @field:Valid
    val images: List<@NotNull Item?>,
) {
    fun toCommand(userId: Long): PresignBatchCommand =
        PresignBatchCommand(
            userId = userId,
            purpose = purpose!!,
            images = images.map { it!!.toCommandItem() },
        )

    data class Item(
        @field:NotBlank
        val contentType: String?,
        @field:NotNull
        @field:Positive
        val contentLength: Long?,
    ) {
        fun toCommandItem(): PresignItem =
            PresignItem(
                contentType = contentType!!,
                contentLength = contentLength!!,
            )
    }
}
