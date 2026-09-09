package com.example.backend.course.adapter.outbound.search

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.course.application.port.inbound.CourseSearchSort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * 코스 검색 커서 코덱(어댑터 소유) 단위 테스트. 정렬 축별 라운드트립(정렬 값 튜플 복원) + 비정상·정렬 불일치 커서 방어.
 */
class CourseSearchCursorCodecTest {
    private fun raw(value: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    @Test
    fun `LATEST 는 createdAt millis 와 id 로 라운드트립한다`() {
        val millis = 1_754_006_400_123L
        val cursor = CourseSearchCursorCodec.encode(CourseSearchSort.LATEST, millis, 7L)

        assertThat(CourseSearchCursorCodec.decode(CourseSearchSort.LATEST, cursor)).containsExactly(millis, 7L)
    }

    @Test
    fun `POPULAR 는 savesCnt 와 id 로 라운드트립한다`() {
        val cursor = CourseSearchCursorCodec.encode(CourseSearchSort.POPULAR, 42L, 9L)

        assertThat(CourseSearchCursorCodec.decode(CourseSearchSort.POPULAR, cursor)).containsExactly(42L, 9L)
    }

    @Test
    fun `RELEVANCE 는 score(double) 와 id 로 라운드트립한다`() {
        val cursor = CourseSearchCursorCodec.encode(CourseSearchSort.RELEVANCE, 3.5, 5L)

        assertThat(CourseSearchCursorCodec.decode(CourseSearchSort.RELEVANCE, cursor)).containsExactly(3.5, 5L)
    }

    @Test
    fun `null 커서는 null 로 디코딩된다`() {
        assertThat(CourseSearchCursorCodec.decode(CourseSearchSort.LATEST, null)).isNull()
    }

    @Test
    fun `커서의 정렬 축이 요청 정렬과 다르면 잘못된 커서다`() {
        val cursor = CourseSearchCursorCodec.encode(CourseSearchSort.LATEST, 0L, 1L)

        // LATEST 로 만든 커서를 POPULAR 로 넘겨 페이지를 이으면 정렬 값 의미가 어긋나므로 막는다.
        assertThatThrownBy { CourseSearchCursorCodec.decode(CourseSearchSort.POPULAR, cursor) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `형식이 틀리면 잘못된 커서다`() {
        assertThatThrownBy { CourseSearchCursorCodec.decode(CourseSearchSort.LATEST, "not-base64!!!") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
        assertThatThrownBy { CourseSearchCursorCodec.decode(CourseSearchSort.LATEST, raw("LATEST:0")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
        // primary 가 숫자가 아니면 거부.
        assertThatThrownBy { CourseSearchCursorCodec.decode(CourseSearchSort.LATEST, raw("LATEST:abc:1")) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(CommonErrorCode.INVALID_INPUT)
    }

    @Test
    fun `RELEVANCE 커서의 primary 가 유한값이 아니면 잘못된 커서다`() {
        // toDoubleOrNull 은 NaN·Infinity 를 통과시키므로 조작된 커서를 막는지 확인한다.
        listOf("NaN", "Infinity", "-Infinity").forEach { bad ->
            assertThatThrownBy { CourseSearchCursorCodec.decode(CourseSearchSort.RELEVANCE, raw("RELEVANCE:$bad:1")) }
                .isInstanceOf(BusinessException::class.java)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.INVALID_INPUT)
        }
    }
}
