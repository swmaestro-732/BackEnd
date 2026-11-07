package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import java.nio.charset.StandardCharsets
import java.util.Base64

/** 내 따라가기 목록의 정렬 키(id)를 URL-safe Base64 불투명 커서로 변환한다([PlanCursorCodec] 과 같은 형식). */
object CourseTraceCursorCodec {
    fun encode(id: Long): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(id.toString().toByteArray(StandardCharsets.UTF_8))

    fun decode(cursor: String?): Long? {
        if (cursor == null) return null
        val decoded =
            try {
                String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            } catch (_: IllegalArgumentException) {
                throw invalidCursor()
            }
        val id = decoded.toLongOrNull() ?: throw invalidCursor()
        if (id <= 0) throw invalidCursor()
        return id
    }

    private fun invalidCursor() = BusinessException(CommonErrorCode.INVALID_INPUT, "잘못된 커서입니다.")
}
