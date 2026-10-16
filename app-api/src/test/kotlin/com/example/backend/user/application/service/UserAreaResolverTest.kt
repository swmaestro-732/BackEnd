package com.example.backend.user.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.area.domain.model.AreaLevel
import com.example.backend.common.exception.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class UserAreaResolverTest {
    private val areaQuery = mock(AreaQueryUseCase::class.java)
    private val resolver = UserAreaResolver(areaQuery)

    private fun descriptor(
        code: String,
        shortName: String,
    ) = AreaDescriptor(prefix = code, shortName = shortName, fullName = shortName, level = AreaLevel.DONG)

    @Test
    fun `normalizeAndValidate — 10자리 코드는 그대로 정규화된다`() {
        val code = "1168010100"
        `when`(areaQuery.findAreaByCode(code)).thenReturn(descriptor(code, "성수동1가"))

        val result = resolver.normalizeAndValidate(listOf(code))

        assertEquals(listOf(code), result)
    }

    @Test
    fun `normalizeAndValidate — 5자리 시군구 코드는 뒤에 00000 을 붙여 10자리로 정규화한다`() {
        val input = "11680"
        val normalized = "1168000000"
        `when`(areaQuery.findAreaByCode(normalized)).thenReturn(descriptor(normalized, "성동구"))

        val result = resolver.normalizeAndValidate(listOf(input))

        assertEquals(listOf(normalized), result)
    }

    @Test
    fun `normalizeAndValidate — 중복 코드는 deduplicate 된다`() {
        val code = "1168010100"
        `when`(areaQuery.findAreaByCode(code)).thenReturn(descriptor(code, "성수동1가"))

        val result = resolver.normalizeAndValidate(listOf(code, code))

        assertEquals(1, result.size)
    }

    @Test
    fun `normalizeAndValidate — 존재하지 않는 코드는 BusinessException 을 던진다`() {
        `when`(areaQuery.findAreaByCode("9999999999")).thenReturn(null)

        assertThrows<BusinessException> { resolver.normalizeAndValidate(listOf("9999999999")) }
    }

    @Test
    fun `resolve — 코드를 이름으로 매핑한다`() {
        val code = "1168010100"
        `when`(areaQuery.findAreaByCode(code)).thenReturn(descriptor(code, "성수동1가"))

        val result = resolver.resolve(listOf(code))

        assertEquals(1, result.size)
        assertEquals(code, result[0].code)
        assertEquals("성수동1가", result[0].name)
    }

    @Test
    fun `resolve — 폐지(null 반환)된 코드는 결과에서 제외된다`() {
        val active = "1168010100"
        val defunct = "9999999999"
        `when`(areaQuery.findAreaByCode(active)).thenReturn(descriptor(active, "성수동1가"))
        `when`(areaQuery.findAreaByCode(defunct)).thenReturn(null)

        val result = resolver.resolve(listOf(active, defunct))

        assertEquals(1, result.size)
        assertEquals(active, result[0].code)
    }

    @Test
    fun `resolve — 빈 목록이면 빈 결과를 반환한다`() {
        val result = resolver.resolve(emptyList())

        assertEquals(emptyList<Any>(), result)
    }
}
