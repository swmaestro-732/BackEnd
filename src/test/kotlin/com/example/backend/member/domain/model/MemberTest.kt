package com.example.backend.member.domain.model

import com.example.backend.common.area.Area
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 도메인 단위 테스트 — Spring 컨텍스트/DB 없이 순수하게 불변식을 검증한다.
 * (헥사고날에서 도메인이 프레임워크에 의존하지 않는 이점.)
 */
class MemberTest {
    @Test
    fun `create 는 신규 Member 를 id 없이 생성한다`() {
        val member = Member.create("hello", Area("서울"))

        assertNull(member.id)
        assertEquals("hello", member.name)
        assertEquals("서울", member.area.value)
    }

    @Test
    fun `이름이 비면 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> { Member.create(" ", Area("서울")) }
    }

    @Test
    fun `이름이 최대 길이를 넘으면 예외를 던진다`() {
        val tooLong = "a".repeat(Member.MAX_NAME_LENGTH + 1)

        assertFailsWith<IllegalArgumentException> { Member.create(tooLong, Area("서울")) }
    }
}
