package com.example.backend.media.application.port.inbound.dto

data class PresignCommand(
    val userId: Long,
    val purpose: UploadPurpose,
    val contentType: String,
)
