package com.example.backend.media.application.port.inbound.dto

/** 업로드 용도 — 키 프리픽스를 결정한다. 확장 시 여기에 케이스만 추가한다. */
enum class UploadPurpose(
    val keyPrefix: String,
) {
    PROFILE("profile"),
}
