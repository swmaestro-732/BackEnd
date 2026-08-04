package com.example.backend.user.application.port.inbound.dto

data class SignupCommand(
    val registrationToken: String,
    val nickname: String,
    val handle: String,
    val profileImageUrl: String?,
)
