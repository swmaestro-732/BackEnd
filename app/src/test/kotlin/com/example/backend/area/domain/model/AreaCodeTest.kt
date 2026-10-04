package com.example.backend.area.domain.model

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AreaCodeTest {
    @Test
    fun `유효한 10자리 숫자 코드를 받아들이고 시도·시군구 prefix 를 파생한다`() {
        val code = AreaCode("1168010100")

        assertAll(
            { assertEquals("1168010100", code.value) },
            { assertEquals("11", code.sidoCode) },
            { assertEquals("11680", code.sigunguCode) },
        )
    }

    @Test
    fun `자릿수가 10이 아니면 예외를 던진다`() {
        assertAll(
            { assertThrows(IllegalArgumentException::class.java) { AreaCode("116801010") } }, // 9자리
            { assertThrows(IllegalArgumentException::class.java) { AreaCode("11680101000") } }, // 11자리
            { assertThrows(IllegalArgumentException::class.java) { AreaCode("") } },
        )
    }

    @Test
    fun `숫자가 아닌 문자가 섞이면 예외를 던진다`() {
        assertAll(
            { assertThrows(IllegalArgumentException::class.java) { AreaCode("1168010A00") } },
            { assertThrows(IllegalArgumentException::class.java) { AreaCode(" 168010100") } },
            { assertThrows(IllegalArgumentException::class.java) { AreaCode("11680-0100") } },
        )
    }
}
