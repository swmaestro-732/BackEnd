package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * 장소 검색 커서 — 어느 경로가 발급했는지 자기 기술한다. 경로마다 페이지네이션 방식이 달라
 * (OpenSearch 는 오프셋, DB LIKE 는 id keyset) 커서를 발급한 경로에서만 이어갈 수 있다.
 */
internal sealed interface PlaceSearchCursor {
    /** OpenSearch from/size 오프셋. [textFallback] 이면 필터 없는 전체 텍스트 질의(0건 폴백)를 이어간다. */
    data class Offset(
        val offset: Int,
        val textFallback: Boolean,
    ) : PlaceSearchCursor

    /** DB LIKE keyset — 이 페이지 마지막 장소 id. */
    data class DbKeyset(
        val lastId: Long,
    ) : PlaceSearchCursor
}

/**
 * [PlaceSearchCursor] 를 URL-safe Base64 불투명 커서로 변환한다([PlaceReviewCursorCodec] 과 같은 형식).
 * 페이로드는 `<mode>:<value>` — os(오프셋)/osf(오프셋·텍스트 폴백)/db(마지막 id).
 * 예전 응답이 마지막 장소 id 를 그대로 커서로 내려줬으므로, 순수 숫자 커서는 DB keyset 으로 관용 해석한다.
 */
internal object PlaceSearchCursorCodec {
    fun encodeOffset(
        offset: Int,
        textFallback: Boolean,
    ): String = encode("${if (textFallback) MODE_OFFSET_FALLBACK else MODE_OFFSET}:$offset")

    fun encodeDbKeyset(lastId: Long): String = encode("$MODE_DB_KEYSET:$lastId")

    fun decode(cursor: String?): PlaceSearchCursor? {
        if (cursor == null) return null

        // 레거시 관용 — 배포 이전 발급분(마지막 장소 id 원문)이 페이지네이션 도중 넘어와도 계속 동작하게 한다.
        cursor.toLongOrNull()?.let {
            if (it <= 0) throw invalidCursor()
            return PlaceSearchCursor.DbKeyset(it)
        }

        val decoded =
            try {
                String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                throw invalidCursor()
            }
        val parts = decoded.split(':')
        if (parts.size != CURSOR_FIELD_COUNT) throw invalidCursor()

        return when (parts[0]) {
            MODE_OFFSET, MODE_OFFSET_FALLBACK -> {
                val offset = parts[1].toIntOrNull() ?: throw invalidCursor()
                if (offset < 1) throw invalidCursor()
                PlaceSearchCursor.Offset(offset = offset, textFallback = parts[0] == MODE_OFFSET_FALLBACK)
            }

            MODE_DB_KEYSET -> {
                val lastId = parts[1].toLongOrNull() ?: throw invalidCursor()
                if (lastId <= 0) throw invalidCursor()
                PlaceSearchCursor.DbKeyset(lastId)
            }

            else -> {
                throw invalidCursor()
            }
        }
    }

    private fun encode(value: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun invalidCursor() = BusinessException(ErrorCode.INVALID_INPUT, "잘못된 커서입니다.")

    private const val MODE_OFFSET = "os"
    private const val MODE_OFFSET_FALLBACK = "osf"
    private const val MODE_DB_KEYSET = "db"
    private const val CURSOR_FIELD_COUNT = 2
}
