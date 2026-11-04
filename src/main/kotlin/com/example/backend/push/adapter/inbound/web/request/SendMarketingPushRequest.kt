package com.example.backend.push.adapter.inbound.web.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 마케팅 푸시 발송 요청 DTO. 링크 형식(https)은 서비스가 검증한다. */
data class SendMarketingPushRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val title: String,
    @field:NotBlank
    @field:Size(max = 200)
    val body: String,
    @field:NotBlank
    val link: String,
)
