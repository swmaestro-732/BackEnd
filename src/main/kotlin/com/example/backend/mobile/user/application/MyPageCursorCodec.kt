package com.example.backend.mobile.user.application

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.mobile.user.application.port.outbound.dto.AuthoredCourseCursor
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

/** 마이페이지 코스 목록의 (createdAt, id) 정렬 키를 URL-safe Base64 불투명 커서로 변환한다. */
internal object MyPageCursorCodec {
    fun encode(cursor: AuthoredCourseCursor): String {
        // created_at 은 epochSecond+nano 로 전체 정밀도 보존 — toEpochMilli 절삭 시 Postgres timestamp(마이크로초)와
        // keyset 의 createdAt eq 비교가 어긋나 같은 밀리초 안의 경계 행이 누락된다.
        val createdAt = cursor.createdAt
        val value = "${createdAt.epochSecond}:${createdAt.nano}:${cursor.id}"
        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(cursor: String?): AuthoredCourseCursor? {
        if (cursor == null) return null

        val decoded =
            try {
                String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                throw invalidCursor()
            }
        val parts = decoded.split(':')
        if (parts.size != CURSOR_FIELD_COUNT) throw invalidCursor()

        val epochSecond = parts[0].toLongOrNull() ?: throw invalidCursor()
        val nano = parts[1].toLongOrNull() ?: throw invalidCursor()
        val id = parts[2].toLongOrNull() ?: throw invalidCursor()
        if (nano < 0 || id <= 0) throw invalidCursor()

        return AuthoredCourseCursor(
            createdAt = Instant.ofEpochSecond(epochSecond, nano),
            id = id,
        )
    }

    private fun invalidCursor() = BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 마이페이지 코스 커서입니다.")

    private const val CURSOR_FIELD_COUNT = 3
}
