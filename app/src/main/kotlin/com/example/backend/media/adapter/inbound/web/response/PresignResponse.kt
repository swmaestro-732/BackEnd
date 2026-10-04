package com.example.backend.media.adapter.inbound.web.response

import com.example.backend.media.application.port.inbound.dto.PresignResult

data class PresignResponse(
    val items: List<Item>,
) {
    data class Item(
        val key: String,
        val uploadUrl: String,
        val imageUrl: String,
    )

    companion object {
        fun from(results: List<PresignResult>): PresignResponse =
            PresignResponse(items = results.map { it.toResponseItem() })

        private fun PresignResult.toResponseItem(): Item =
            Item(
                key = key,
                uploadUrl = uploadUrl,
                imageUrl = imageUrl,
            )
    }
}
