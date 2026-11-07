package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

/** 따라가기 목록 커서 코덱 단위 테스트 — id 라운드트립 + 비정상 커서 방어([PlanCursorCodecTest] 와 같은 형식). */
class CourseTraceCursorCodecTest {
    @Test
    fun `encode-decode 라운드트립은 id 를 그대로 돌려준다`() {
        assertThat(CourseTraceCursorCodec.decode(CourseTraceCursorCodec.encode(42L))).isEqualTo(42L)
        assertThat(
            CourseTraceCursorCodec.decode(CourseTraceCursorCodec.encode(Long.MAX_VALUE)),
        ).isEqualTo(Long.MAX_VALUE)
    }

    @Test
    fun `null 커서는 null 로 디코딩된다`() {
        assertThat(CourseTraceCursorCodec.decode(null)).isNull()
    }

    @Test
    fun `Base64 가 아니면 잘못된 커서다`() {
        assertInvalid("not a cursor!!")
    }

    @Test
    fun `숫자가 아니면 잘못된 커서다`() {
        assertInvalid(raw("abc"))
        assertInvalid(raw("1:2"))
        assertInvalid(raw(""))
    }

    @Test
    fun `id 가 양수가 아니면 잘못된 커서다`() {
        assertInvalid(raw("0"))
        assertInvalid(raw("-1"))
    }

    private fun assertInvalid(cursor: String) {
        assertThatThrownBy { CourseTraceCursorCodec.decode(cursor) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    private fun raw(value: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))
}
