package com.example.backend.place.application.service

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.place.domain.model.PlaceCategory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

class PlaceSearchQueryPlannerTest {
    private val areas = mock(AreaQueryUseCase::class.java)
    private val planner = PlaceSearchQueryPlanner(areas)

    @Test
    fun `서울 카페는 자동완성 대신 검색용 시도 prefix와 카테고리로 해석한다`() {
        `when`(areas.resolveSearchPrefixes("서울")).thenReturn(listOf("11"))

        assertEquals(
            PlaceSearchPlan(emptyList(), listOf(PlaceCategory.CAFE), listOf(listOf("11"))),
            planner.plan("서울 카페"),
        )
        verify(areas).resolveSearchPrefixes("서울")
        verifyNoMoreInteractions(areas)
    }

    @Test
    fun `지역과 카테고리에 매칭되지 않는 토큰은 텍스트로 남긴다`() {
        `when`(areas.resolveSearchPrefixes("서울")).thenReturn(listOf("11"))
        `when`(areas.resolveSearchPrefixes("블루보틀")).thenReturn(emptyList())

        assertEquals(
            PlaceSearchPlan(listOf("블루보틀"), listOf(PlaceCategory.CAFE), listOf(listOf("11"))),
            planner.plan("서울 블루보틀 카페"),
        )
    }

    @Test
    fun `서울과 강남구는 서로 다른 지역 조건으로 유지한다`() {
        `when`(areas.resolveSearchPrefixes("서울")).thenReturn(listOf("11"))
        `when`(areas.resolveSearchPrefixes("강남구")).thenReturn(listOf("11680"))

        val plan = planner.plan("서울 강남구 카페")

        assertEquals(listOf(listOf("11"), listOf("11680")), plan.areaCodePrefixGroups)
        assertEquals(listOf(PlaceCategory.CAFE), plan.categories)
        assertEquals(emptyList<String>(), plan.textTokens)
    }

    @Test
    fun `서울과 성수는 성수동 후보를 서울 prefix에 합치지 않는다`() {
        `when`(areas.resolveSearchPrefixes("서울")).thenReturn(listOf("11"))
        `when`(areas.resolveSearchPrefixes("성수")).thenReturn(listOf("1120011400", "1120011500"))

        val plan = planner.plan("서울 성수 카페")

        assertEquals(listOf(listOf("11"), listOf("1120011400", "1120011500")), plan.areaCodePrefixGroups)
    }

    @Test
    fun `같은 토큰의 중복과 상위 지역에 포함된 후보만 제거한다`() {
        `when`(areas.resolveSearchPrefixes("중구")).thenReturn(listOf("11140", "1114010100", "26110", "11140"))
        `when`(areas.resolveSearchPrefixes("서울")).thenReturn(listOf("11"))

        val plan = planner.plan("서울 중구 카페")

        assertEquals(listOf(listOf("11"), listOf("11140", "26110")), plan.areaCodePrefixGroups)
    }
}
