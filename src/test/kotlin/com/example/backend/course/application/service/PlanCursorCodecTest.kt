package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.course.application.port.outbound.PlanCursor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

/**
 * 계획 목록 커서 코덱 단위 테스트 — 라운드트립(정밀도 보존) + 비정상 커서 방어
 * ([CourseReviewCursorCodecTest] 와 같은 형식, 정렬이 고정이라 정렬 태그는 없다).
 */
class PlanCursorCodecTest {
    @Test
    fun `encode-decode 라운드트립은 마이크로초 정밀도까지 보존한다`() {
        val cursor = PlanCursor(updatedAt = Instant.parse("2026-09-18T08:00:00.123456Z"), id = 7)

        val decoded = PlanCursorCodec.decode(PlanCursorCodec.encode(cursor))

        assertThat(decoded).isEqualTo(cursor)
        assertThat(decoded!!.updatedAt.nano).isEqualTo(123_456_000)
    }

    @Test
    fun `null 커서는 null 로 디코딩된다`() {
        assertThat(PlanCursorCodec.decode(null)).isNull()
    }

    @Test
    fun `Base64 가 아니면 잘못된 커서다`() {
        assertInvalid("not a cursor!!")
    }

    @Test
    fun `필드 수가 다르면 잘못된 커서다`() {
        assertInvalid(raw("1789880501:824940000"))
        assertInvalid(raw("1789880501:824940000:2:3"))
    }

    @Test
    fun `숫자가 아닌 값이 섞이면 잘못된 커서다`() {
        assertInvalid(raw("abc:0:2"))
        assertInvalid(raw("1789880501:xyz:2"))
        assertInvalid(raw("1789880501:0:id"))
    }

    @Test
    fun `나노초 범위나 id 가 유효하지 않으면 잘못된 커서다`() {
        assertInvalid(raw("1789880501:1000000000:2")) // 나노초 상한 초과
        assertInvalid(raw("1789880501:-1:2"))
        assertInvalid(raw("1789880501:0:0")) // id 는 양수여야 한다
    }

    @Test
    fun `Instant 범위를 벗어난 초는 500 이 아니라 400 으로 막는다`() {
        assertInvalid(raw("999999999999999999:0:2"))
    }

    private fun assertInvalid(cursor: String) {
        assertThatThrownBy { PlanCursorCodec.decode(cursor) }
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
