package com.example.backend.user.application.port.inbound.dto

data class UpdateProfileCommand(
    val nickname: String?,
    val handle: String?,
    val profileImageUrl: String?,
)
