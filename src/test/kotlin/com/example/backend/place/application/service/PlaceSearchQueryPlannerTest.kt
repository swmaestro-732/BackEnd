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
            PlaceSearchPlan(emptyList(), listOf(PlaceCategory.CAFE), listOf("11")),
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
            PlaceSearchPlan(listOf("블루보틀"), listOf(PlaceCategory.CAFE), listOf("11")),
            planner.plan("서울 블루보틀 카페"),
        )
    }
}
