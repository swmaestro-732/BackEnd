package com.example.backend.area.adapter.outbound.persistence

import com.example.backend.area.adapter.outbound.persistence.exposed.repository.AreaRepository
import com.example.backend.area.domain.model.Area
import com.example.backend.area.domain.model.AreaCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AreaPersistenceAdapterTest {
    @Test
    fun `서울 검색은 자동완성 20건 제한과 무관하게 서울 전체 prefix를 반환한다`() {
        val areas = (100..124).map { area("11${it}10100", "서울특별시", "테스트${it}구", "테스트동") }
        val adapter = adapter(areas)

        assertEquals(20, adapter.search("서울").size)
        assertEquals(listOf("11"), adapter.resolveSearchPrefixes("서울"))
        assertTrue(areas.all { area -> adapter.resolveSearchPrefixes("서울").any { area.code.value.startsWith(it) } })
    }

    @Test
    fun `같은 동명이 20건을 넘더라도 모든 지역 코드를 반환한다`() {
        val areas = (100..124).map { area("99${it}10100", "검증도", "테스트${it}군", "중앙동") }
        val adapter = adapter(areas)

        assertEquals(20, adapter.search("중앙동").size)
        assertEquals(areas.map { it.code.value }.toSet(), adapter.resolveSearchPrefixes("중앙동").toSet())
        assertEquals(25, adapter.resolveSearchPrefixes("중앙동").size)
    }

    @Test
    fun `시군구는 하위 동을 포함하는 prefix로 합치고 동 검색은 해당 동만 반환한다`() {
        val adapter =
            adapter(
                listOf(
                    area("1168010100", "서울특별시", "강남구", "역삼동"),
                    area("1168010500", "서울특별시", "강남구", "삼성동"),
                    area("1120011400", "서울특별시", "성동구", "성수동1가"),
                    area("1120011500", "서울특별시", "성동구", "성수동2가"),
                ),
            )

        assertEquals(listOf("11680"), adapter.resolveSearchPrefixes("강남"))
        assertEquals(listOf("11680"), adapter.resolveSearchPrefixes("서울특별시 강남구"))
        assertEquals(listOf("1168010100"), adapter.resolveSearchPrefixes(" 역삼 "))
        assertEquals(listOf("1168010100"), adapter.resolveSearchPrefixes("강남구 역삼동"))
        assertEquals(setOf("1120011400", "1120011500"), adapter.resolveSearchPrefixes("성수").toSet())
    }

    @Test
    fun `시군구가 없는 세종도 시도와 동 검색을 지원한다`() {
        val adapter = adapter(listOf(area("3611010100", "세종특별자치시", null, "반곡동")))

        assertEquals(listOf("36"), adapter.resolveSearchPrefixes("세종"))
        assertEquals(listOf("3611010100"), adapter.resolveSearchPrefixes("반곡"))
    }

    @Test
    fun `빈 검색어와 일치하지 않는 검색어는 필터를 만들지 않는다`() {
        val adapter = adapter(listOf(area("1168010100", "서울특별시", "강남구", "역삼동")))

        assertTrue(adapter.resolveSearchPrefixes("  ").isEmpty())
        assertTrue(adapter.resolveSearchPrefixes("없는지역").isEmpty())
    }

    private fun adapter(areas: List<Area>): AreaPersistenceAdapter {
        val repository = mock(AreaRepository::class.java)
        `when`(repository.findAllActive()).thenReturn(areas)
        return AreaPersistenceAdapter(repository)
    }

    private fun area(
        code: String,
        sido: String,
        sigungu: String?,
        dong: String,
    ) = Area(AreaCode(code), sido, sigungu, dong, isActive = true)
}
