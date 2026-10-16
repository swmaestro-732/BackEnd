package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * 장소 검색 커서 — 검색엔진 from/size 오프셋. [textFallback] 이면 필터 없는 전체 텍스트 질의(0건 폴백)를 이어가고,
 * [anchorPlaceId] 는 발급 당시 기준 장소(다음 페이지에서 바뀌면 400).
 */
internal data class PlaceSearchCursor(
    val offset: Int,
    val textFallback: Boolean,
    val anchorPlaceId: Long? = null,
)

/**
 * [PlaceSearchCursor] 를 URL-safe Base64 불투명 커서로 변환한다(리뷰·피드 커서와 같은 형식).
 * 페이로드는 `<os|osf>:<offset>[:<anchorPlaceId>]`.
 */
internal object PlaceSearchCursorCodec {
    fun encode(
        offset: Int,
        textFallback: Boolean,
        anchorPlaceId: Long? = null,
    ): String {
        val mode = if (textFallback) MODE_OFFSET_FALLBACK else MODE_OFFSET
        val value = "$mode:$offset" + (anchorPlaceId?.let { ":$it" } ?: "")
        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(cursor: String?): PlaceSearchCursor? {
        if (cursor == null) return null

        val decoded =
            try {
                String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                throw invalidCursor()
            }
        val parts = decoded.split(':')
        if (parts.size !in 2..3) throw invalidCursor()
        if (parts[0] != MODE_OFFSET && parts[0] != MODE_OFFSET_FALLBACK) throw invalidCursor()
        val offset = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: throw invalidCursor()
        val anchor = if (parts.size == 3) parts[2].toLongOrNull()?.takeIf { it > 0 } ?: throw invalidCursor() else null

        return PlaceSearchCursor(
            offset = offset,
            textFallback = parts[0] == MODE_OFFSET_FALLBACK,
            anchorPlaceId = anchor,
        )
    }

    private fun invalidCursor() = BusinessException(CommonErrorCode.INVALID_INPUT, "잘못된 커서입니다.")

    private const val MODE_OFFSET = "os"
    private const val MODE_OFFSET_FALLBACK = "osf"
}
