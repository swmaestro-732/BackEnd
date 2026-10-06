package com.example.backend.place.adapter.inbound.web.response

import com.example.backend.place.application.port.inbound.dto.PlaceMapCluster
import com.example.backend.place.application.port.inbound.dto.PlaceMapResult
import com.example.backend.place.application.port.inbound.dto.PlaceSummary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlaceMapResponseTest {
    @Test
    fun `from() - 개별 마커와 클러스터를 순서 그대로 옮기고 imageUrl·address 는 내리지 않는다`() {
        val result =
            PlaceMapResult(
                totalCount = 101L,
                places =
                    listOf(
                        PlaceSummary(
                            7L,
                            "대림창고",
                            "CULTURE",
                            "https://img/7.jpg",
                            37.5418,
                            127.0592,
                            "서울 성동구",
                            "1120011400",
                        ),
                    ),
                clusters = listOf(PlaceMapCluster("wydm9q", 37.5441, 127.0563, 100L)),
            )

        val response = PlaceMapResponse.from(result)

        assertThat(response.totalCount).isEqualTo(101L)
        assertThat(response.places).containsExactly(PlaceMapMarkerResponse(7L, "대림창고", "CULTURE", 37.5418, 127.0592))
        assertThat(response.clusters).containsExactly(PlaceMapClusterResponse("wydm9q", 37.5441, 127.0563, 100L))
    }
}
