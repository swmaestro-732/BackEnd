package com.example.backend.place.adapter.outbound.persistence

import com.example.backend.place.adapter.outbound.persistence.exposed.repository.PlaceRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class PlaceQueryAdapterTest {
    private val repository: PlaceRepository = mock(PlaceRepository::class.java)
    private val adapter = PlaceQueryAdapter(repository)

    @Test
    fun `findPlacesById - 빈 목록은 리포지토리를 호출하지 않고 빈 리스트를 반환한다`() {
        val result = adapter.findPlacesById(emptyList())

        assertThat(result).isEmpty()
        verifyNoInteractions(repository)
    }

    @Test
    fun `searchByName - 공백 검색어는 리포지토리를 호출하지 않고 빈 리스트를 반환한다`() {
        val result = adapter.searchByName("", null, 10)

        assertThat(result).isEmpty()
        verifyNoInteractions(repository)
    }

    @Test
    fun `searchByName - 공백만 있는 검색어는 리포지토리를 호출하지 않고 빈 리스트를 반환한다`() {
        val result = adapter.searchByName("   ", null, 10)

        assertThat(result).isEmpty()
        verifyNoInteractions(repository)
    }

    @Test
    fun `countByName - 공백 검색어는 리포지토리를 호출하지 않고 0을 반환한다`() {
        val count = adapter.countByName("")

        assertThat(count).isEqualTo(0L)
        verifyNoInteractions(repository)
    }

    @Test
    fun `countByName - 공백만 있는 검색어는 0을 반환한다`() {
        val count = adapter.countByName("  ")

        assertThat(count).isEqualTo(0L)
        verifyNoInteractions(repository)
    }
}
