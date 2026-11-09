package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.course.application.port.outbound.FeedCursor
import java.nio.charset.StandardCharsets
import java.time.DateTimeException
import java.time.Instant
import java.util.Base64

/** 추천 커서 — 엔진 페이지([Engine]: 감쇠 기준 시각·점수·id)와 DB 폴백 페이지([Feed]: 공개 피드 키셋)를 구분한다. */
internal sealed interface CourseRecommendCursor {
    data class Engine(
        val origin: Instant,
        val score: Double,
        val id: Long,
    ) : CourseRecommendCursor

    data class Feed(
        val value: FeedCursor,
    ) : CourseRecommendCursor
}

/**
 * 추천 커서를 URL-safe Base64 불투명 문자열로 변환한다.
 * 페이로드는 `os:originMillis:score:id` 또는 `db:savesCnt:epochSecond:nano:id` — 출처 접두사로 한 번 시작한 경로를 끝까지 유지한다.
 */
internal object CourseRecommendCursorCodec {
    fun encode(cursor: CourseRecommendCursor): String {
        val value =
            when (cursor) {
                is CourseRecommendCursor.Engine -> {
                    "$ENGINE:${cursor.origin.toEpochMilli()}:${cursor.score}:${cursor.id}"
                }

                is CourseRecommendCursor.Feed -> {
                    // created_at 은 epochSecond+nano 로 전체 정밀도 보존 — 밀리초 절삭은 keyset 경계 행을 누락시킨다.
                    val createdAt = cursor.value.createdAt
                    "$FEED:${cursor.value.savesCnt}:${createdAt.epochSecond}:${createdAt.nano}:${cursor.value.id}"
                }
            }
        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    /** null 커서(첫 페이지)는 null. 형식 오류는 400. */
    fun decode(cursor: String?): CourseRecommendCursor? {
        if (cursor == null) return null
        val decoded =
            try {
                String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                throw invalidCursor()
            }
        val parts = decoded.split(':')
        return when (parts.firstOrNull()) {
            ENGINE -> decodeEngine(parts)
            FEED -> decodeFeed(parts)
            else -> throw invalidCursor()
        }
    }

    private fun decodeEngine(parts: List<String>): CourseRecommendCursor.Engine {
        if (parts.size != ENGINE_FIELD_COUNT) throw invalidCursor()
        val originMillis = parts[1].toLongOrNull() ?: throw invalidCursor()
        // toDoubleOrNull 은 "NaN"/"Infinity" 도 통과시키므로 유한값만 허용한다(조작된 커서 방어).
        val score = parts[2].toDoubleOrNull()?.takeIf(Double::isFinite) ?: throw invalidCursor()
        val id = parts[3].toLongOrNull() ?: throw invalidCursor()
        if (id <= 0) throw invalidCursor()
        val origin =
            try {
                Instant.ofEpochMilli(originMillis)
            } catch (_: DateTimeException) {
                throw invalidCursor()
            }
        return CourseRecommendCursor.Engine(origin, score, id)
    }

    private fun decodeFeed(parts: List<String>): CourseRecommendCursor.Feed {
        if (parts.size != FEED_FIELD_COUNT) throw invalidCursor()
        val savesCnt = parts[1].toIntOrNull() ?: throw invalidCursor()
        val epochSecond = parts[2].toLongOrNull() ?: throw invalidCursor()
        val nano = parts[3].toLongOrNull() ?: throw invalidCursor()
        val id = parts[4].toLongOrNull() ?: throw invalidCursor()
        if (savesCnt < 0 || nano !in 0L..NANOS_MAX || id <= 0) throw invalidCursor()
        val createdAt =
            try {
                Instant.ofEpochSecond(epochSecond, nano)
            } catch (_: DateTimeException) {
                throw invalidCursor()
            }
        return CourseRecommendCursor.Feed(FeedCursor(savesCnt = savesCnt, createdAt = createdAt, id = id))
    }

    private fun invalidCursor() = BusinessException(CommonErrorCode.INVALID_INPUT, "유효하지 않은 추천 코스 커서입니다.")

    private const val ENGINE = "os"
    private const val FEED = "db"
    private const val ENGINE_FIELD_COUNT = 4
    private const val FEED_FIELD_COUNT = 5
    private const val NANOS_MAX = 999_999_999L
}
