package com.example.backend.place.adapter.inbound.web.response

import com.example.backend.place.application.port.inbound.dto.PlaceMapCluster
import com.example.backend.place.application.port.inbound.dto.PlaceMapMarker
import com.example.backend.place.application.port.inbound.dto.PlaceMapResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PlaceMapResponseTest {
    @Test
    fun `from() - 개별 마커와 클러스터를 순서 그대로 옮긴다`() {
        val result =
            PlaceMapResult(
                totalCount = 101L,
                places = listOf(PlaceMapMarker(7L, "대림창고", "CULTURE", "서울 성동구 성수이로 78", 37.5418, 127.0592)),
                clusters = listOf(PlaceMapCluster("wydm9q", 37.5441, 127.0563, 100L)),
            )

        val response = PlaceMapResponse.from(result)

        assertThat(response.totalCount).isEqualTo(101L)
        assertThat(response.places).containsExactly(
            PlaceMapMarkerResponse(7L, "대림창고", "CULTURE", "서울 성동구 성수이로 78", 37.5418, 127.0592),
        )
        assertThat(response.clusters).containsExactly(PlaceMapClusterResponse("wydm9q", 37.5441, 127.0563, 100L))
    }
}
