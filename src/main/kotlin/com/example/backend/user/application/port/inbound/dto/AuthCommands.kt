package com.example.backend.user.application.port.inbound.dto

import com.example.backend.user.domain.model.SocialProvider

data class SocialLoginCommand(
    val provider: SocialProvider,
    val idToken: String,
)

data class SignupCommand(
    val registrationToken: String,
    val nickname: String,
    val handle: String,
    val profileImageUrl: String?,
)
