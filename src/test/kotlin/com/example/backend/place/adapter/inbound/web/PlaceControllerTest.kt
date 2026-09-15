package com.example.backend.place.adapter.inbound.web

import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummaryPage
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class PlaceControllerTest : IntegrationTestBase() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var placeQueryUseCase: PlaceQueryUseCase

    @Test
    fun `GET places - mock=true 는 고정 장소 목록을 반환한다`() {
        mockMvc
            .perform(get("/api/v1/places?mock=true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.places").isArray)
    }

    @Test
    fun `GET places - 검색어로 실구현 검색하면 200을 반환한다`() {
        val emptyPage = PlaceSummaryPage(items = emptyList(), totalCount = 0, hasNext = false)
        `when`(
            placeQueryUseCase.searchByName(anyString(), nullable(String::class.java), anyInt()),
        ).thenReturn(emptyPage)

        mockMvc
            .perform(get("/api/v1/places?q=카페&size=10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalCount").value(0))
            .andExpect(jsonPath("$.data.hasNext").value(false))
    }

    @Test
    fun `GET places - 파라미터 없이 호출하면 기본값으로 실구현 검색한다`() {
        val emptyPage = PlaceSummaryPage(items = emptyList(), totalCount = 0, hasNext = false)
        `when`(
            placeQueryUseCase.searchByName(anyString(), nullable(String::class.java), anyInt()),
        ).thenReturn(emptyPage)

        mockMvc
            .perform(get("/api/v1/places"))
            .andExpect(status().isOk)
    }
}
