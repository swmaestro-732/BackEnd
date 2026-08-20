package com.example.backend.place.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * [PlaceReviewTag] 단위 테스트 — 태그 마스터 테이블이 없어진 뒤 이 enum 이 코드 정본이다(V4).
 * 저장 값(enum 이름)과 API 코드(소문자)의 대응, 코드 → enum 변환 규칙을 고정한다.
 */
class PlaceReviewTagTest {
    @Test
    fun `코드는 enum 이름의 소문자다`() {
        assertEquals("coffee", PlaceReviewTag.COFFEE.code)
        assertEquals("nowait", PlaceReviewTag.NOWAIT.code)
        assertEquals("rainyday", PlaceReviewTag.RAINYDAY.code)
    }

    @Test
    fun `taxonomy 장소 리뷰 태그 64종을 모두 들고 있고 코드가 유일하다`() {
        // `.ai/taxonomy.md` "장소 리뷰 태그"(공통 14 + 업종별 50)가 정본 — 값이 늘면 이 테스트도 함께 고친다.
        assertEquals(64, PlaceReviewTag.entries.size)
        assertEquals(
            PlaceReviewTag.entries.size,
            PlaceReviewTag.entries
                .map { it.code }
                .distinct()
                .size,
        )
    }

    @Test
    fun `모든 태그가 문구와 아이콘을 갖는다`() {
        // 태그를 그리는 쪽이 마스터 조회 없이 code → 문구·이모지를 채울 수 있어야 한다.
        assertEquals(
            emptyList<PlaceReviewTag>(),
            PlaceReviewTag.entries.filter {
                it.label.isBlank() ||
                    it.icon.isBlank()
            },
        )
    }

    @Test
    fun `코드로 태그를 찾는다`() {
        assertSame(PlaceReviewTag.COFFEE, PlaceReviewTag.fromCodeOrNull("coffee"))
    }

    @Test
    fun `대문자·앞뒤 공백이 섞인 코드도 받아준다`() {
        assertSame(PlaceReviewTag.VIEW, PlaceReviewTag.fromCodeOrNull(" VIEW "))
    }

    @Test
    fun `모르는 코드는 null 이다`() {
        // 호출부(PlaceReviewService)가 이 null 을 400 으로 바꾼다.
        assertNull(PlaceReviewTag.fromCodeOrNull("nosuchtag"))
        assertNull(PlaceReviewTag.fromCodeOrNull(""))
    }
}
