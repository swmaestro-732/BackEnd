package com.example.backend.mobile.user.application

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.mobile.user.application.port.outbound.dto.AuthoredCourseCursor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

/**
 * 마이페이지 코스 커서 코덱 단위 테스트. 라운드트립(정밀도 보존) + 비정상 커서 방어.
 * 비정상 nano/epochSecond 가 Instant 정규화·DateTimeException 으로 새지 않고 잘못된 커서(INVALID_INPUT)로 처리되는지 확인한다.
 */
class MyPageCursorCodecTest {
    private fun raw(value: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    @Test
    fun `encode-decode 라운드트립은 마이크로초 정밀도까지 보존한다`() {
        val cursor = AuthoredCourseCursor(createdAt = Instant.parse("2026-08-01T00:00:00.123456Z"), id = 7)

        val decoded = MyPageCursorCodec.decode(MyPageCursorCodec.encode(cursor))

        assertThat(decoded).isEqualTo(cursor)
        assertThat(decoded!!.createdAt.nano).isEqualTo(123_456_000)
    }

    @Test
    fun `null 커서는 null 로 디코딩된다`() {
        assertThat(MyPageCursorCodec.decode(null)).isNull()
    }

    @Test
    fun `nano 가 10억 이상이면 잘못된 커서다`() {
        assertThatThrownBy { MyPageCursorCodec.decode(raw("0:1000000000:1")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `epochSecond 가 Instant 범위를 벗어나면 잘못된 커서다`() {
        assertThatThrownBy { MyPageCursorCodec.decode(raw("9223372036854775807:0:1")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `필드 수나 형식이 틀리면 잘못된 커서다`() {
        assertThatThrownBy { MyPageCursorCodec.decode("not-base64!!!") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
        assertThatThrownBy { MyPageCursorCodec.decode(raw("0:1")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }
}
