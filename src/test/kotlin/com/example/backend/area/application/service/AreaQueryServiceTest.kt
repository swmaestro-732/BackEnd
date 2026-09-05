package com.example.backend.area.application.service

import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.area.application.port.outbound.AreaDirectoryPort
import com.example.backend.area.domain.model.AreaLevel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AreaQueryServiceTest {
    private val fakePort =
        object : AreaDirectoryPort {
            var searchResult: List<AreaDescriptor> = emptyList()
            var areaByCode: AreaDescriptor? = null
            var searchedKeyword: String? = null
            var searchedCode: String? = null

            override fun search(keyword: String): List<AreaDescriptor> {
                searchedKeyword = keyword
                return searchResult
            }

            override fun findByCode(code: String): AreaDescriptor? {
                searchedCode = code
                return areaByCode
            }
        }
    private val service = AreaQueryService(fakePort)

    @Test
    fun `지역 검색어와 결과를 그대로 전달한다`() {
        val expected = listOf(area("11200", "성동구", AreaLevel.SIGUNGU))
        fakePort.searchResult = expected

        assertEquals(expected, service.searchAreas("성동"))
        assertEquals("성동", fakePort.searchedKeyword)
    }

    @Test
    fun `코드에 해당하는 지역이 없으면 null을 반환한다`() {
        assertNull(service.findAreaByCode("9999999999"))
        assertEquals("9999999999", fakePort.searchedCode)
    }

    private fun area(
        prefix: String,
        name: String,
        level: AreaLevel,
    ) = AreaDescriptor(prefix, name, "서울특별시 $name", level)
}
