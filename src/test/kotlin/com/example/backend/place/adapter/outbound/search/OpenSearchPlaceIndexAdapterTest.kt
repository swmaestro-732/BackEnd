package com.example.backend.place.adapter.outbound.search

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.domain.model.Place
import com.example.backend.place.domain.model.PlaceBusinessStatus
import com.example.backend.place.domain.model.PlaceCategory
import com.example.backend.place.domain.model.PlaceStatus
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.ObjectProvider

class OpenSearchPlaceIndexAdapterTest {
    private val clientProvider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
    private val adapter = OpenSearchPlaceIndexAdapter(clientProvider)

    private fun placeWithId(id: Long): Place =
        Place.reconstitute(
            id = id,
            status = PlaceStatus.ACTIVE,
            name = "테스트 장소",
            description = null,
            category = PlaceCategory.CAFE,
            location = Coordinate(latitude = 37.5445, longitude = 127.0578),
            address = "서울 성동구 테스트로 1",
            areaCode = null,
            imageUrl = null,
            businessStatus = PlaceBusinessStatus.UNKNOWN,
            kakaoPlaceId = null,
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
        )

    private fun placeWithNullId(): Place =
        Place.create(
            name = "미저장 장소",
            description = null,
            category = PlaceCategory.CAFE,
            location = Coordinate(latitude = 37.5445, longitude = 127.0578),
            address = "서울 성동구 테스트로 2",
            imageUrl = null,
        )

    @Test
    fun `빈 목록을 넘기면 client 를 조회하지 않는다`() {
        adapter.save(emptyList())

        verify(clientProvider, never()).ifAvailable
    }

    @Test
    fun `client 가 없으면(endpoint 미설정) no-op 이고 예외가 전파되지 않는다`() {
        org.mockito.Mockito
            .`when`(clientProvider.ifAvailable)
            .thenReturn(null)

        adapter.save(listOf(placeWithId(1L))) // no exception
    }

    @Test
    fun `id 가 null 인 장소만 있으면 예외 없이 완료된다(early return)`() {
        val client = mock(OpenSearchClient::class.java)
        org.mockito.Mockito
            .`when`(clientProvider.ifAvailable)
            .thenReturn(client)

        // documents filter removes all null-id places → early return before bulk
        adapter.save(listOf(placeWithNullId())) // no exception
    }

    @Test
    fun `bulk 응답이 null 이어도(mock 기본값) 예외가 전파되지 않는다(fail-soft)`() {
        val client = mock(OpenSearchClient::class.java)
        org.mockito.Mockito
            .`when`(clientProvider.ifAvailable)
            .thenReturn(client)
        // client.bulk returns null (Mockito default) → response.errors() throws NPE
        // → caught by catch block → no propagation

        adapter.save(listOf(placeWithId(1L))) // no exception
    }

    @Test
    fun `id 있는 장소와 null-id 장소가 섞여 있어도 예외가 전파되지 않는다`() {
        val client = mock(OpenSearchClient::class.java)
        org.mockito.Mockito
            .`when`(clientProvider.ifAvailable)
            .thenReturn(client)

        adapter.save(listOf(placeWithId(1L), placeWithNullId(), placeWithId(2L))) // no exception
    }
}
