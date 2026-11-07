package com.example.backend.push.application.port.outbound

import com.example.backend.push.application.port.inbound.dto.PushMessage

/** 최대 500개 토큰에 푸시를 발송하고 결과를 반환한다. */
interface PushSenderPort {
    fun send(message: PushMessage): PushSendResult
}

data class PushSendResult(
    val successCount: Int,
    val invalidTokens: List<String>,
    val failedCount: Int,
) {
    override fun toString(): String =
        "PushSendResult(successCount=$successCount, invalidTokenCount=${invalidTokens.size}, failedCount=$failedCount)"
}
