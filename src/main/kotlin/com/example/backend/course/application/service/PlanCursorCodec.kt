package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.course.application.port.outbound.PlanCursor
import java.nio.charset.StandardCharsets
import java.time.DateTimeException
import java.time.Instant
import java.util.Base64

/**
 * 내 계획 목록의 정렬 키(updatedAt·id)를 URL-safe Base64 불투명 커서로 변환한다
 * ([CourseReviewCursorCodec] 과 같은 형식, 정렬 고정이라 정렬 태그는 없다).
 * updated_at 은 epochSecond+nano 로 전체 정밀도를 보존한다 — 밀리초로 절삭하면 keyset 의 eq 비교가 어긋나 경계 행이 누락된다.
 */
internal object PlanCursorCodec {
    fun encode(cursor: PlanCursor): String {
        val value = "${cursor.updatedAt.epochSecond}:${cursor.updatedAt.nano}:${cursor.id}"
        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(cursor: String?): PlanCursor? {
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
        if (nano !in 0L..NANOS_MAX || id <= 0) throw invalidCursor()

        return PlanCursor(
            updatedAt =
                try {
                    Instant.ofEpochSecond(epochSecond, nano)
                } catch (_: DateTimeException) {
                    throw invalidCursor()
                },
            id = id,
        )
    }

    private fun invalidCursor() = BusinessException(CommonErrorCode.INVALID_INPUT, "잘못된 커서입니다.")

    private const val CURSOR_FIELD_COUNT = 3
    private const val NANOS_MAX = 999_999_999L
}
