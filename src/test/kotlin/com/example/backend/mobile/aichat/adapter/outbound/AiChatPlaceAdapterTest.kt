package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.area.application.port.inbound.AreaQueryUseCase
import com.example.backend.area.application.port.inbound.dto.AreaDescriptor
import com.example.backend.area.domain.model.AreaLevel
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.mobile.aichat.application.port.outbound.AiChatPlace
import com.example.backend.place.application.port.inbound.PlaceQueryUseCase
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import com.example.backend.place.application.port.inbound.dto.PlaceSummaryPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/** [AiChatPlaceAdapter] — 장소 검색 위임, 카테고리 라벨·동 이름 매핑, 엔진 장애(503) 흡수를 검증한다. */
class AiChatPlaceAdapterTest {
    private val placeQueryUseCase = mock(PlaceQueryUseCase::class.java)
    private val areaQueryUseCase = mock(AreaQueryUseCase::class.java)
    private val adapter = AiChatPlaceAdapter(placeQueryUseCase, areaQueryUseCase)

    /** Mockito matcher 는 null 을 돌려주므로 Kotlin non-null 파라미터에는 제네릭으로 감싸 넘긴다. */
    private fun <T> anyArg(): T = ArgumentMatchers.any()

    private fun <T> eqArg(value: T): T = ArgumentMatchers.eq(value)

    private fun place(
        id: Long,
        name: String,
        category: String,
        areaCode: String?,
    ) = PlaceSummary(
        id = id,
        name = name,
        category = category,
        imageUrl = null,
        latitude = 37.5,
        longitude = 127.0,
        address = "서울",
        areaCode = areaCode,
    )

    @Test
    fun `이름으로 상위 후보를 찾아 라벨과 동 이름을 붙인다`() {
        `when`(placeQueryUseCase.searchByName(eqArg("어니언"), isNull(), eqArg(5), isNull(), eqArg(listOf("1120011400"))))
            .thenReturn(
                PlaceSummaryPage(
                    items =
                        listOf(
                            place(10, "어니언 성수", "CAFE", "1120011400"),
                            place(11, "어니언 안국", "UNKNOWN_CODE", null),
                        ),
                    totalCount = 2,
                    hasNext = false,
                ),
            )
        `when`(areaQueryUseCase.findAreaByCode("1120011400"))
            .thenReturn(AreaDescriptor("1120011400", "성수동2가", "서울특별시 성동구 성수동2가", AreaLevel.DONG))

        val result = adapter.searchByName("어니언", 5, listOf("1120011400"))

        assertThat(result).containsExactly(
            AiChatPlace(10L, "어니언 성수", "카페·디저트", "성수동2가"),
            AiChatPlace(11L, "어니언 안국", "UNKNOWN_CODE", null),
        )
    }

    @Test
    fun `장소 검색이 비즈니스 예외로 실패하면 빈 목록이다`() {
        `when`(placeQueryUseCase.searchByName(anyArg(), isNull(), anyInt(), isNull(), anyArg()))
            .thenThrow(BusinessException(CommonErrorCode.INVALID_INPUT))

        assertThat(adapter.searchByName("어니언", 5, emptyList())).isEmpty()
    }
}
