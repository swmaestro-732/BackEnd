package com.example.backend.course.adapter.outbound.search

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.course.application.port.inbound.CourseSearchSort
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * OpenSearch search_after 정렬 값을 URL-safe Base64 **불투명 커서**로 변환한다(검색 엔진 세부라 어댑터 소유).
 * 페이로드는 `정렬기준:primary:id` — primary 는 정렬 축 값(relevance=score double, latest=createdAt millis, popular=savesCnt),
 * id 는 tiebreak. 디코딩 시 커서의 정렬기준이 요청 정렬과 다르면(정렬 바꿔 페이지 넘김) 잘못된 커서로 막는다(400).
 */
internal object CourseSearchCursorCodec {
    fun encode(
        sort: CourseSearchSort,
        primary: Any,
        id: Long,
    ): String {
        val value = "${sort.name}:$primary:$id"
        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
    }

    /** 커서를 정렬 값 튜플([primary, id])로 되돌린다. null 커서(첫 페이지)는 null. 형식·정렬 불일치는 400. */
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
                // toDoubleOrNull 은 "NaN"/"Infinity" 도 통과시키므로 유한값만 허용한다(조작된 커서 방어).
                CourseSearchSort.RELEVANCE -> {
                    parts[1].toDoubleOrNull()?.takeIf(Double::isFinite)
                        ?: throw invalidCursor()
                }

                CourseSearchSort.LATEST, CourseSearchSort.POPULAR -> {
                    parts[1].toLongOrNull() ?: throw invalidCursor()
                }
            }
        val id = parts[2].toLongOrNull() ?: throw invalidCursor()
        if (id <= 0) throw invalidCursor()

        return listOf(primary, id)
    }

    private fun invalidCursor() = BusinessException(CommonErrorCode.INVALID_INPUT, "유효하지 않은 코스 검색 커서입니다.")

    private const val CURSOR_FIELD_COUNT = 3
}
