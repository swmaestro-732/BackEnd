package com.example.backend.bootstrap.search

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.course.application.port.inbound.CourseReindexUseCase
import com.example.backend.place.application.port.inbound.PlaceReindexUseCase
import com.example.backend.user.application.port.inbound.SavedPlaceReindexUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.CountRequest
import org.opensearch.client.opensearch.core.CountResponse
import org.opensearch.client.util.ObjectBuilder
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.DefaultApplicationArguments
import java.util.function.Function

class OpenSearchReindexRunnerTest {
    private val client = mock(OpenSearchClient::class.java)
    private val place = mock(PlaceReindexUseCase::class.java)
    private val course = mock(CourseReindexUseCase::class.java)
    private val savedPlace = mock(SavedPlaceReindexUseCase::class.java)

    @Suppress("UNCHECKED_CAST")
    private val provider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
    private val counts = mutableMapOf("dev-place" to 1L, "dev-course" to 1L, "dev-saved_place" to 0L)
    private val aliases = mutableListOf<String>()

    @Test
    fun `클라이언트가 없으면 재색인하지 않는다`() {
        runner().run(DefaultApplicationArguments())

        verifyNoInteractions(place, course, savedPlace)
    }

    @Test
    fun `접두사가 붙은 saved_place 가 비었을 때만 backfill 한다`() {
        stubCounts()

        runner().run(DefaultApplicationArguments())

        assertEquals(listOf("dev-place", "dev-course", "dev-saved_place"), aliases)
        verify(savedPlace).reindexAll()
        verifyNoInteractions(place, course)
    }

    @Test
    fun `모든 인덱스가 채워졌으면 자동 backfill 을 생략한다`() {
        counts["dev-saved_place"] = 1
        stubCounts()

        runner().run(DefaultApplicationArguments())

        verifyNoInteractions(place, course, savedPlace)
    }

    @Test
    fun `강제 재색인은 기존 문서가 있어도 saved_place 를 포함한다`() {
        counts["dev-saved_place"] = 1
        stubCounts()

        runner(force = true).run(DefaultApplicationArguments())

        verify(place).reindexAll()
        verify(course).reindexAll()
        verify(savedPlace).reindexAll()
    }

    @Test
    fun `앞 타겟의 실패와 saved_place 재색인 실패는 부팅을 막지 않는다`() {
        stubCounts()
        `when`(place.reindexAll()).thenThrow(IllegalStateException("place unavailable"))
        `when`(savedPlace.reindexAll()).thenThrow(IllegalStateException("saved_place unavailable"))

        runner(force = true).run(DefaultApplicationArguments())

        verify(course).reindexAll()
        verify(savedPlace).reindexAll()
    }

    private fun runner(force: Boolean = false) =
        OpenSearchReindexRunner(
            provider,
            OpenSearchProperties(indexPrefix = "dev-", reindexOnStartup = force),
            place,
            course,
            savedPlace,
        )

    private fun stubCounts() {
        `when`(provider.ifAvailable).thenReturn(client)
        `when`(client.count(anyArg<Function<CountRequest.Builder, ObjectBuilder<CountRequest>>>()))
            .thenAnswer { invocation ->
                val request =
                    invocation
                        .getArgument<Function<CountRequest.Builder, ObjectBuilder<CountRequest>>>(0)
                        .apply(CountRequest.Builder())
                        .build()
                val alias = request.index().single()
                aliases += alias
                CountResponse
                    .Builder()
                    .count(counts.getValue(alias))
                    .shards { it.total(1).successful(1).failed(0) }
                    .build()
            }
    }

    private fun <T> anyArg(): T = ArgumentMatchers.any()
}
