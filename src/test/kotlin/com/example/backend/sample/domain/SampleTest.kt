package com.example.backend.sample.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 도메인 단위 테스트 — Spring 컨텍스트/DB 없이 순수하게 불변식을 검증한다.
 * (헥사고날에서 도메인이 프레임워크에 의존하지 않는 이점.)
 */
class SampleTest {
    @Test
    fun `create 는 신규 Sample 을 id 없이 생성한다`() {
        val sample = Sample.create("hello")

        assertNull(sample.id)
        assertEquals("hello", sample.name)
    }

    @Test
    fun `이름이 비면 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> { Sample.create(" ") }
    }

    @Test
    fun `이름이 최대 길이를 넘으면 예외를 던진다`() {
        val tooLong = "a".repeat(Sample.MAX_NAME_LENGTH + 1)

        assertFailsWith<IllegalArgumentException> { Sample.create(tooLong) }
    }
}
