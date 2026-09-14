package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.place.application.port.inbound.dto.PlaceReviewSortKey
import com.example.backend.place.application.port.outbound.PlaceReviewCursor
import java.nio.charset.StandardCharsets
import java.time.DateTimeException
import java.time.Instant
import java.util.Base64

/**
 * 장소 리뷰 목록의 정렬 키(평점·작성일·id)를 URL-safe Base64 불투명 커서로 변환한다
 * ([com.example.backend.mobile.home.application.service.HomeFeedCursorCodec] 와 같은 형식).
 * 페이로드 앞에 정렬 기준·방향을 담아, 디코딩 시 요청 정렬과 다르면(정렬 바꿔 페이지 넘김) 잘못된 커서로 막는다(400).
 * 클라이언트는 값을 해석하지 않고 그대로 되돌려 준다.
 */
internal object PlaceReviewCursorCodec {
    fun encode(
        sort: PlaceReviewSortKey,
        descending: Boolean,
        cursor: PlaceReviewCursor,
    ): String {
        // created_at 은 epochSecond+nano 로 전체 정밀도를 보존한다 — 밀리초로 절삭하면
        // Postgres timestamp(마이크로초)와 keyset 의 createdAt eq 비교가 어긋나 경계 행이 누락된다.
        val createdAt = cursor.createdAt
        val order = orderName(descending)
        val value = "${sort.name}:$order:${cursor.rating}:${createdAt.epochSecond}:${createdAt.nano}:${cursor.id}"
        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(
        sort: PlaceReviewSortKey,
        descending: Boolean,
        cursor: String?,
    ): PlaceReviewCursor? {
        if (cursor == null) return null

        val decoded =
            try {
                String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                throw invalidCursor()
            }
        val parts = decoded.split(':')
        if (parts.size != CURSOR_FIELD_COUNT) throw invalidCursor()
        if (parts[0] != sort.name || parts[1] != orderName(descending)) throw invalidCursor()

        val rating = parts[2].toIntOrNull() ?: throw invalidCursor()
        val epochSecond = parts[3].toLongOrNull() ?: throw invalidCursor()
        val nano = parts[4].toLongOrNull() ?: throw invalidCursor()
        val id = parts[5].toLongOrNull() ?: throw invalidCursor()
        if (rating !in 1..5 || nano !in 0L..NANOS_MAX || id <= 0) throw invalidCursor()

        return PlaceReviewCursor(
            rating = rating,
            // 범위 밖 epochSecond 는 Instant.ofEpochSecond 가 DateTimeException 을 던져 500 이 된다 — 400 으로 바꾼다.
            createdAt =
                try {
                    Instant.ofEpochSecond(epochSecond, nano)
                } catch (_: DateTimeException) {
                    throw invalidCursor()
                },
            id = id,
        )
    }

    private fun orderName(descending: Boolean) = if (descending) "DESC" else "ASC"

    private fun invalidCursor() = BusinessException(CommonErrorCode.INVALID_INPUT, "잘못된 커서입니다.")

    private const val CURSOR_FIELD_COUNT = 6
    private const val NANOS_MAX = 999_999_999L
}
