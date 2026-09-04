package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.course.application.port.inbound.CourseSearchSort
import com.example.backend.course.application.port.outbound.CourseSearchHit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

/**
 * 코스 검색 커서 코덱 단위 테스트. 정렬 축별 라운드트립(정렬 값 튜플 복원) + 비정상·정렬 불일치 커서 방어.
 */
class CourseSearchCursorCodecTest {
    private fun raw(value: String): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun hit(
        id: Long,
        savesCnt: Int,
        createdAt: Instant,
        score: Double?,
    ) = CourseSearchHit(
        id = id,
        authorId = 1,
        title = "코스",
        coverImageUrl = null,
        theme = null,
        area = null,
        likesCnt = 0,
        savesCnt = savesCnt,
        createdAt = createdAt,
        score = score,
    )

    @Test
    fun `LATEST 는 createdAt millis 와 id 로 라운드트립한다`() {
        val createdAt = Instant.parse("2026-08-01T00:00:00.123Z")
        val cursor =
            CourseSearchCursorCodec.encode(
                CourseSearchSort.LATEST,
                hit(id = 7, savesCnt = 3, createdAt = createdAt, score = null),
            )

        val decoded = CourseSearchCursorCodec.decode(CourseSearchSort.LATEST, cursor)

        assertThat(decoded).containsExactly(createdAt.toEpochMilli(), 7L)
    }

    @Test
    fun `POPULAR 는 savesCnt 와 id 로 라운드트립한다`() {
        val cursor =
            CourseSearchCursorCodec.encode(
                CourseSearchSort.POPULAR,
                hit(id = 9, savesCnt = 42, createdAt = Instant.EPOCH, score = null),
            )

        assertThat(CourseSearchCursorCodec.decode(CourseSearchSort.POPULAR, cursor)).containsExactly(42L, 9L)
    }

    @Test
    fun `RELEVANCE 는 score(double) 와 id 로 라운드트립한다`() {
        val cursor =
            CourseSearchCursorCodec.encode(
                CourseSearchSort.RELEVANCE,
                hit(id = 5, savesCnt = 0, createdAt = Instant.EPOCH, score = 3.5),
            )

        assertThat(CourseSearchCursorCodec.decode(CourseSearchSort.RELEVANCE, cursor)).containsExactly(3.5, 5L)
    }

    @Test
    fun `null 커서는 null 로 디코딩된다`() {
        assertThat(CourseSearchCursorCodec.decode(CourseSearchSort.LATEST, null)).isNull()
    }

    @Test
    fun `커서의 정렬 축이 요청 정렬과 다르면 잘못된 커서다`() {
        val cursor =
            CourseSearchCursorCodec.encode(
                CourseSearchSort.LATEST,
                hit(id = 1, savesCnt = 0, createdAt = Instant.EPOCH, score = null),
            )

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
}
