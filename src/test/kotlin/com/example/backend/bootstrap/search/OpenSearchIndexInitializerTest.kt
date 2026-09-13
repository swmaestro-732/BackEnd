package com.example.backend.bootstrap.search

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.DefaultApplicationArguments

class OpenSearchIndexInitializerTest {
    private val clientProvider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
    private val initializer = OpenSearchIndexInitializer(clientProvider)
    private val args = DefaultApplicationArguments()

    @Test
    fun `endpoint 미설정(client 없음)이면 아무것도 하지 않는다`() {
        `when`(clientProvider.ifAvailable).thenReturn(null)

        initializer.run(args) // no exception

        verify(clientProvider, never()).getObject()
    }

    @Test
    fun `client 가 있으면 indices() 호출 중 NPE 가 발생해도 예외가 전파되지 않는다(fail-soft)`() {
        val client = mock(OpenSearchClient::class.java)
        `when`(clientProvider.ifAvailable).thenReturn(client)
        // client.indices() returns null (Mockito default) → indices().existsAlias(...) throws NPE
        // → caught by forEach catch block → warn logged → no propagation

        initializer.run(args) // no exception
    }
}
