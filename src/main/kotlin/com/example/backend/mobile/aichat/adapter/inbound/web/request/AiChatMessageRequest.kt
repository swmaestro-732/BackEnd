package com.example.backend.mobile.aichat.adapter.inbound.web.request

import com.example.backend.common.geo.Coordinate
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatInputType
import com.example.backend.mobile.aichat.application.port.inbound.dto.AiChatMessageCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 커미 AI 에게 보내는 사용자 답변 한 턴. 첫 답변(지역)은 [sessionId] 없이 보내고 서버가 세션을 만든다.
 * [Type.CHIP] 이면 [value], [Type.TEXT] 면 [text] 가 있어야 한다(없으면 4002).
 */
data class AiChatMessageRequest(
    val sessionId: String? = null,
    @field:NotNull
    val type: Type?,
    /** 직전 응답 quickReplies 의 value. 첫 답변의 "내 근처"는 `NEAR_ME`. */
    @field:Size(max = 100)
    val value: String? = null,
    /** 직접 입력 문장 또는 앱 지역 칩 문구. 앞뒤 공백 제거 후 1~100자. */
    @field:Size(max = 100)
    val text: String? = null,
    /** `NEAR_ME` 또는 requiresLocation 칩일 때 현재 좌표. 위치 권한이 없으면 null. */
    @field:Valid
    val location: Location? = null,
) {
    enum class Type { CHIP, TEXT }

    data class Location(
        @field:DecimalMin("-90.0")
        @field:DecimalMax("90.0")
        val latitude: Double,
        @field:DecimalMin("-180.0")
        @field:DecimalMax("180.0")
        val longitude: Double,
    )

    @get:AssertTrue(message = "CHIP 은 value, TEXT 는 text 가 필요합니다")
    val payloadPresent: Boolean
        get() =
            when (type) {
                Type.CHIP -> !value.isNullOrBlank()
                Type.TEXT -> !text.isNullOrBlank()
                null -> true
            }

    fun toCommand(userId: Long): AiChatMessageCommand =
        AiChatMessageCommand(
            userId = userId,
            sessionId = sessionId,
            type = if (type == Type.CHIP) AiChatInputType.CHIP else AiChatInputType.TEXT,
            value = value?.trim(),
            text = text?.trim(),
            location = location?.let { Coordinate(it.latitude, it.longitude) },
        )
}
