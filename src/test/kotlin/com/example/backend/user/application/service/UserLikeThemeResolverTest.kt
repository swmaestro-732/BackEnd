package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.user.application.port.outbound.LikeThemePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class UserLikeThemeResolverTest {
    private val port = mock(LikeThemePort::class.java)
    private val resolver = UserLikeThemeResolver(port)

    private val validThemes = listOf("CAFETOUR", "FOOD", "CULTURE", "SHOPPING")

    @Test
    fun `빈 목록은 port 를 조회하지 않고 빈 결과를 반환한다`() {
        val result = resolver.validate(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `유효한 테마 목록은 그대로 반환한다`() {
        `when`(port.listThemeNames()).thenReturn(validThemes)

        val result = resolver.validate(listOf("CAFETOUR", "FOOD"))

        assertEquals(listOf("CAFETOUR", "FOOD"), result)
    }

    @Test
    fun `중복된 테마는 deduplicate 된 목록을 반환한다`() {
        `when`(port.listThemeNames()).thenReturn(validThemes)

        val result = resolver.validate(listOf("CAFETOUR", "CAFETOUR", "FOOD"))

        assertEquals(listOf("CAFETOUR", "FOOD"), result)
    }

    @Test
    fun `존재하지 않는 테마가 포함되면 BusinessException 을 던진다`() {
        `when`(port.listThemeNames()).thenReturn(validThemes)

        assertThrows<BusinessException> { resolver.validate(listOf("CAFETOUR", "UNKNOWN_THEME")) }
    }
}
