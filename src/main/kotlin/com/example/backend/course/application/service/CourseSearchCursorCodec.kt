package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.course.application.port.inbound.CourseSearchSort
import com.example.backend.course.application.port.outbound.CourseSearchHit
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * 코스 검색의 search_after 정렬 값을 URL-safe Base64 불투명 커서로 변환한다.
 * 페이로드는 `정렬기준:primary:id` — primary 는 정렬 축 값(relevance=score double, latest=createdAt millis, popular=savesCnt),
 * id 는 tiebreak 다. 디코딩 시 커서에 박힌 정렬기준이 요청 정렬과 다르면(정렬 바꿔 페이지 넘김) 잘못된 커서로 막는다.
 */
internal object CourseSearchCursorCodec {
    fun encode(
        sort: CourseSearchSort,
        hit: CourseSearchHit,
    ): String {
        val primary =
            when (sort) {
                CourseSearchSort.RELEVANCE -> (hit.score ?: 0.0).toString()
                CourseSearchSort.LATEST -> hit.createdAt.toEpochMilli().toString()
                CourseSearchSort.POPULAR -> hit.savesCnt.toString()
            }
        val value = "${sort.name}:$primary:${hit.id}"
        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    /** 커서를 정렬 값 튜플([primary, id])로 되돌린다. null 커서(첫 페이지)는 null. */
    fun decode(
        sort: CourseSearchSort,
        cursor: String?,
    ): List<Any>? {
        if (cursor == null) return null

        val decoded =
            try {
                String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                throw invalidCursor()
            }
        val parts = decoded.split(':')
        if (parts.size != CURSOR_FIELD_COUNT) throw invalidCursor()
        if (parts[0] != sort.name) throw invalidCursor()

        val primary: Any =
            when (sort) {
                CourseSearchSort.RELEVANCE -> parts[1].toDoubleOrNull() ?: throw invalidCursor()
                CourseSearchSort.LATEST, CourseSearchSort.POPULAR -> parts[1].toLongOrNull() ?: throw invalidCursor()
            }
        val id = parts[2].toLongOrNull() ?: throw invalidCursor()
        if (id <= 0) throw invalidCursor()

        return listOf(primary, id)
    }

    private fun invalidCursor() = BusinessException(CommonErrorCode.INVALID_INPUT, "유효하지 않은 코스 검색 커서입니다.")

    private const val CURSOR_FIELD_COUNT = 3
}
