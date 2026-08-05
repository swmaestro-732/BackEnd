package com.example.backend.media.application.port.inbound.dto

data class PresignBatchCommand(
    val userId: Long,
    val purpose: UploadPurpose,
    val images: List<PresignItem>,
)

data class PresignItem(
    val contentType: String,
    val contentLength: Long,
)
