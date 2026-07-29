package com.example.backend.media.adapter.inbound.web.response

import com.example.backend.media.application.port.inbound.dto.PresignResult

data class PresignResponse(
    val key: String,
    val uploadUrl: String,
    val imageUrl: String,
) {
    companion object {
        fun from(result: PresignResult): PresignResponse =
            PresignResponse(
                key = result.key,
                uploadUrl = result.uploadUrl,
                imageUrl = result.imageUrl,
            )
    }
}
