package com.example.backend.place.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.place.application.port.outbound.PlaceReviewCursor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

/**
 * 장소 리뷰 커서 코덱 단위 테스트. 라운드트립(정밀도 보존) + 비정상 커서 방어
 * ([com.example.backend.mobile.home.application.service.HomeFeedCursorCodecTest] 와 같은 형식).
 */
class PlaceReviewCursorCodecTest {
    private fun raw(value: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    @Test
    fun `encode-decode 라운드트립은 마이크로초 정밀도까지 보존한다`() {
        val cursor = PlaceReviewCursor(rating = 4, createdAt = Instant.parse("2026-08-01T00:00:00.123456Z"), id = 7)

        val decoded = PlaceReviewCursorCodec.decode(PlaceReviewCursorCodec.encode(cursor))

        assertThat(decoded).isEqualTo(cursor)
        assertThat(decoded!!.createdAt.nano).isEqualTo(123_456_000)
    }

    @Test
    fun `null 커서는 null 로 디코딩된다`() {
        assertThat(PlaceReviewCursorCodec.decode(null)).isNull()
    }

    @Test
    fun `별점이 1~5 를 벗어나면 잘못된 커서다`() {
        assertThatThrownBy { PlaceReviewCursorCodec.decode(raw("0:0:0:1")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
        assertThatThrownBy { PlaceReviewCursorCodec.decode(raw("6:0:0:1")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `nano 가 10억 이상이면 잘못된 커서다`() {
        // 1_000_000_000 은 ofEpochSecond 가 초로 정규화해 다른 경계를 만들 수 있어 거부한다.
        assertThatThrownBy { PlaceReviewCursorCodec.decode(raw("4:0:1000000000:1")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `epochSecond 가 Instant 범위를 벗어나면 잘못된 커서다`() {
        // Long.MAX_VALUE epochSecond 는 DateTimeException → 500 이 아니라 400 으로 변환돼야 한다.
        assertThatThrownBy { PlaceReviewCursorCodec.decode(raw("4:9223372036854775807:0:1")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `필드 수나 형식이 틀리면 잘못된 커서다`() {
        assertThatThrownBy { PlaceReviewCursorCodec.decode("not-base64!!!") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
        assertThatThrownBy { PlaceReviewCursorCodec.decode(raw("4:0:0")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }
}
