package com.example.backend.user.application.port.inbound.dto

data class LoginResult(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isNewUser: Boolean,
    val registrationToken: String? = null,
)

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

data class SignupResult(
    val accessToken: String,
    val refreshToken: String,
    val user: SignupUserResult,
)

data class SignupUserResult(
    val id: Long,
    val nickname: String,
    val handle: String,
    val profileImageUrl: String?,
)
