package com.example.backend.user.application.dto

/** 유스케이스 입력 — 웹/외부 형식과 분리된 애플리케이션 경계 타입. */
data class CreateUserCommand(
    val nickname: String,
)
