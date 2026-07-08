package com.example.backend.user.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 도메인 단위 테스트 — Spring 컨텍스트/DB 없이 순수하게 불변식을 검증한다.
 * (헥사고날에서 도메인이 프레임워크에 의존하지 않는 이점.)
 */
class UserTest {
    @Test
    fun `create 는 신규 User 를 id 없이 생성한다`() {
        val user = User.create("hello")

        assertNull(user.id)
        assertEquals("hello", user.nickname)
    }

    @Test
    fun `닉네임이 비면 예외를 던진다`() {
        assertFailsWith<IllegalArgumentException> { User.create(" ") }
    }

    @Test
    fun `닉네임이 최대 길이를 넘으면 예외를 던진다`() {
        val tooLong = "a".repeat(User.MAX_NICKNAME_LENGTH + 1)

        assertFailsWith<IllegalArgumentException> { User.create(tooLong) }
    }
}
