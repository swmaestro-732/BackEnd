package com.example.backend.media.application.port.inbound.dto

data class PresignResult(
    val key: String,
    val uploadUrl: String,
    val imageUrl: String,
)
