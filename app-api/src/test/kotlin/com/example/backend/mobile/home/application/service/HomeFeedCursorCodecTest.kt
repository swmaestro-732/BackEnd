package com.example.backend.mobile.home.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCursor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

/**
 * 피드 복합 커서 코덱 단위 테스트. 라운드트립(정밀도 보존) + 비정상 커서 방어.
 * 비정상 nano/epochSecond 커서가 [Instant.ofEpochSecond] 정규화나 DateTimeException 으로 새지 않고
 * 모두 잘못된 커서(INVALID_INPUT)로 처리되는지 확인한다.
 */
class HomeFeedCursorCodecTest {
    private fun raw(value: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    @Test
    fun `encode-decode 라운드트립은 마이크로초 정밀도까지 보존한다`() {
        val cursor = HomeFeedCursor(savesCnt = 342, createdAt = Instant.parse("2026-08-01T00:00:00.123456Z"), id = 7)

        val decoded = HomeFeedCursorCodec.decode(HomeFeedCursorCodec.encode(cursor))

        assertThat(decoded).isEqualTo(cursor)
        assertThat(decoded!!.createdAt.nano).isEqualTo(123_456_000)
    }

    @Test
    fun `null 커서는 null 로 디코딩된다`() {
        assertThat(HomeFeedCursorCodec.decode(null)).isNull()
    }

    @Test
    fun `nano 가 10억 이상이면 잘못된 커서다`() {
        // 1_000_000_000 은 ofEpochSecond 가 초로 정규화해 다른 경계를 만들 수 있어 거부한다.
        assertThatThrownBy { HomeFeedCursorCodec.decode(raw("0:0:1000000000:1")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `epochSecond 가 Instant 범위를 벗어나면 잘못된 커서다`() {
        // Long.MAX_VALUE epochSecond 는 DateTimeException → 500 이 아니라 400 으로 변환돼야 한다.
        assertThatThrownBy { HomeFeedCursorCodec.decode(raw("0:9223372036854775807:0:1")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `필드 수나 형식이 틀리면 잘못된 커서다`() {
        assertThatThrownBy { HomeFeedCursorCodec.decode("not-base64!!!") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
        assertThatThrownBy { HomeFeedCursorCodec.decode(raw("0:0:0")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }
}
