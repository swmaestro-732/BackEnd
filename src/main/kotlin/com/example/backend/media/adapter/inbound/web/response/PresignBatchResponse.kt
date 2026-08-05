package com.example.backend.media.adapter.inbound.web.response

import com.example.backend.media.application.port.inbound.dto.PresignResult

data class PresignBatchResponse(
    val items: List<PresignResponse>,
) {
    companion object {
        fun from(results: List<PresignResult>): PresignBatchResponse =
            PresignBatchResponse(items = results.map { PresignResponse.from(it) })
    }
}
