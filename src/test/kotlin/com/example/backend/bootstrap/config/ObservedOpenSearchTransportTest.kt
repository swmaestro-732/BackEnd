package com.example.backend.bootstrap.config

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opensearch.client.json.JsonpMapper
import org.opensearch.client.opensearch.core.SearchRequest
import org.opensearch.client.transport.Endpoint
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.TransportOptions
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException

/** OpenSearch 호출이 관측(span, 타이머)을 남기는지 확인한다(SCRUM-769). */
class ObservedOpenSearchTransportTest {
    private val stopped = mutableListOf<Observation.Context>()
    private val registry =
        ObservationRegistry.create().apply {
            observationConfig().observationHandler(
                object : ObservationHandler<Observation.Context> {
                    override fun supportsContext(context: Observation.Context) = true

                    override fun onStop(context: Observation.Context) {
                        stopped.add(context)
                    }
                },
            )
        }
    private val search = SearchRequest.of { it.index("place") }

    /** 호출 시점의 현재 관측을 기록하고, [result] 대로 응답하거나 던지는 가짜 전송. */
    private class FakeTransport(
        private val registry: ObservationRegistry,
        private val result: () -> Any?,
    ) : OpenSearchTransport {
        var current: Observation? = null

        @Suppress("UNCHECKED_CAST")
        override fun <RequestT, ResponseT, ErrorT> performRequest(
            request: RequestT,
            endpoint: Endpoint<RequestT, ResponseT, ErrorT>,
            options: TransportOptions?,
        ): ResponseT {
            current = registry.currentObservation
            return result() as ResponseT
        }

        @Suppress("UNCHECKED_CAST")
        override fun <RequestT, ResponseT, ErrorT> performRequestAsync(
            request: RequestT,
            endpoint: Endpoint<RequestT, ResponseT, ErrorT>,
            options: TransportOptions?,
        ): CompletableFuture<ResponseT> {
            current = registry.currentObservation
            return result() as CompletableFuture<ResponseT>
        }

        override fun jsonpMapper(): JsonpMapper = throw UnsupportedOperationException()

        override fun options(): TransportOptions? = null

        override fun close() = Unit
    }

    @Test
    fun `동기 호출은 요청 타입 이름과 인덱스로 관측을 남기고 호출 중 현재 관측이 된다`() {
        val fake = FakeTransport(registry) { "ok" }
        val parent = Observation.start("request", registry)

        parent.openScope().use {
            ObservedOpenSearchTransport(fake, registry).performRequest(search, SearchRequest._ENDPOINT, null)
        }
        parent.stop()

        val context = stopped.first()
        assertEquals(ObservedOpenSearchTransport.NAME, context.name)
        assertEquals("opensearch search", context.contextualName)
        assertEquals("search", context.getLowCardinalityKeyValue("db.operation")?.value)
        assertEquals("place", context.getHighCardinalityKeyValue("db.namespace")?.value)
        assertSame(context, fake.current?.context)
        assertSame(parent, context.parentObservation)
        assertNull(context.error)
    }

    @Test
    fun `동기 호출 실패는 에러를 기록하고 그대로 던진다`() {
        val fake = FakeTransport(registry) { throw IOException("down") }

        assertThrows<IOException> {
            ObservedOpenSearchTransport(fake, registry).performRequest(search, SearchRequest._ENDPOINT, null)
        }

        assertInstanceOf(IOException::class.java, stopped.single().error)
    }

    @Test
    fun `비동기 호출은 완료될 때 관측을 닫고 감싼 예외를 풀어 기록한다`() {
        val future = CompletableFuture<Any?>()
        val fake = FakeTransport(registry) { future }

        val result =
            ObservedOpenSearchTransport(
                fake,
                registry,
            ).performRequestAsync(search, SearchRequest._ENDPOINT, null)
        assertEquals(emptyList<Observation.Context>(), stopped)

        future.completeExceptionally(CompletionException(IOException("down")))

        assertThrows<ExecutionException> { result.get() }
        assertInstanceOf(IOException::class.java, stopped.single().error)
    }

    @Test
    fun `비동기 호출이 future 없이 바로 던져도 관측을 닫는다`() {
        val fake = FakeTransport(registry) { throw IllegalStateException("serialize") }

        assertThrows<IllegalStateException> {
            ObservedOpenSearchTransport(fake, registry).performRequestAsync(search, SearchRequest._ENDPOINT, null)
        }

        assertInstanceOf(IllegalStateException::class.java, stopped.single().error)
    }

    @Test
    fun `이름 규칙`() {
        assertEquals("search", ObservedOpenSearchTransport.operationName(search))
        assertEquals("unknown", ObservedOpenSearchTransport.operationName(null))
        assertEquals("unknown", ObservedOpenSearchTransport.operationName(object {}))
        assertEquals("place", ObservedOpenSearchTransport.indexName("/place/_doc/1:2"))
        assertEquals("none", ObservedOpenSearchTransport.indexName("/_bulk"))
        assertEquals("none", ObservedOpenSearchTransport.indexName("/"))
    }

    @Test
    fun `검색, 색인 클라이언트는 관측 전송을 쓰고 헬스 인디케이터는 쓰지 않는다`() {
        val config =
            OpenSearchConfig(OpenSearchProperties(endpoint = "http://127.0.0.1:1", username = "u", password = "p"))
        val transport = config.openSearchTransport()
        try {
            assertInstanceOf(
                ObservedOpenSearchTransport::class.java,
                config.openSearchClient(transport, registry)._transport(),
            )
            assertInstanceOf(
                ObservedOpenSearchTransport::class.java,
                config.openSearchIndexClient(transport, registry)._transport(),
            )
            // 헬스 체크는 실패해도 UNKNOWN 이고, 관측을 남기지 않는다.
            config.openSearchHealthIndicator(transport).health()
            assertEquals(emptyList<Observation.Context>(), stopped)
        } finally {
            transport.close()
        }
    }
}
