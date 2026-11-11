package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.course.application.port.outbound.FeedCursor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

/** [CourseRecommendCursorCodec] — 엔진(os)·DB(db) 커서 왕복과 조작·형식 오류 거절을 검증한다. */
class CourseRecommendCursorCodecTest {
    private fun encodeRaw(payload: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.toByteArray(StandardCharsets.UTF_8))

    private fun decodeRaw(cursor: String): String =
        String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)

    @Test
    fun `null 커서는 null 로 푼다`() {
        assertThat(CourseRecommendCursorCodec.decode(null)).isNull()
    }

    @Test
    fun `엔진 커서는 os 접두사로 origin·score·id 를 왕복한다`() {
        val origin = Instant.parse("2026-11-03T12:34:56.789Z")
        val cursor = CourseRecommendCursor.Engine(origin, 12.5, 42L)

        val encoded = CourseRecommendCursorCodec.encode(cursor)

        assertThat(decodeRaw(encoded)).isEqualTo("os:${origin.toEpochMilli()}:12.5:42")
        assertThat(CourseRecommendCursorCodec.decode(encoded)).isEqualTo(cursor)
    }

    @Test
    fun `DB 커서는 db 접두사로 created_at 나노 정밀도까지 왕복한다`() {
        val createdAt = Instant.ofEpochSecond(1_700_000_000L, 123_456_789L)
        val cursor = CourseRecommendCursor.Feed(FeedCursor(savesCnt = 7, createdAt = createdAt, id = 99L))

        val encoded = CourseRecommendCursorCodec.encode(cursor)

        assertThat(decodeRaw(encoded)).isEqualTo("db:7:1700000000:123456789:99")
        assertThat(CourseRecommendCursorCodec.decode(encoded)).isEqualTo(cursor)
    }

    @Test
    fun `Base64 가 아니면 INVALID_INPUT`() {
        assertInvalid("not-a-valid-cursor!!")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "", // 접두사 없음
            "xx:1:2:3", // 알 수 없는 출처
            "os:1:2", // 엔진 필드 수 부족
            "os:1:2:3:4", // 엔진 필드 수 초과
            "os:abc:2.0:3", // origin 숫자 아님
            "os:1:abc:3", // score 숫자 아님
            "os:1:NaN:3", // 유한값 아님
            "os:1:Infinity:3",
            "os:1:2.0:abc", // id 숫자 아님
            "os:1:2.0:0", // id 양수 아님
            "db:1:2:3", // DB 필드 수 부족
            "db:1:2:3:4:5", // DB 필드 수 초과
            "db:abc:2:3:4", // savesCnt 숫자 아님
            "db:-1:2:3:4", // savesCnt 음수
            "db:1:abc:3:4", // epochSecond 숫자 아님
            "db:1:2:abc:4", // nano 숫자 아님
            "db:1:2:1000000000:4", // nano 범위 초과
            "db:1:2:3:abc", // id 숫자 아님
            "db:1:2:3:0", // id 양수 아님
            "db:1:9223372036854775807:0:4", // Instant 범위 초과
        ],
    )
    fun `형식이 어긋난 페이로드는 INVALID_INPUT`(payload: String) {
        assertInvalid(encodeRaw(payload))
    }

    private fun assertInvalid(cursor: String) {
        assertThatThrownBy { CourseRecommendCursorCodec.decode(cursor) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }
}
