package com.example.backend.bootstrap.config

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.opensearch.client.transport.Endpoint
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.TransportOptions
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * OpenSearch 호출마다 `opensearch.client.requests` 관측(트레이스 span + 타이머)을 남긴다(SCRUM-769).
 * opensearch-java 전송은 JDBC, RestClient 계측에 걸리지 않아, 검색 지연이 트레이스에서 구간으로 보이지 않았다.
 * 이름은 요청 타입(SearchRequest → search)만 써 카디널리티를 묶는다. 경로는 문서 id(저장 장소는 `userId:placeId`)를
 * 담을 수 있어 인덱스 이름만 span 속성에 남긴다.
 * 수명은 감싼 transport 빈이 관리하므로 [close] 는 위임하지 않는다.
 */
class ObservedOpenSearchTransport(
    private val delegate: OpenSearchTransport,
    private val registry: ObservationRegistry,
) : OpenSearchTransport by delegate {
    override fun <RequestT, ResponseT, ErrorT> performRequest(
        request: RequestT,
        endpoint: Endpoint<RequestT, ResponseT, ErrorT>,
        options: TransportOptions?,
    ): ResponseT {
        val observation = observation(request, endpoint).start()
        return try {
            observation.openScope().use { delegate.performRequest(request, endpoint, options) }
        } catch (e: Throwable) {
            observation.error(e)
            throw e
        } finally {
            observation.stop()
        }
    }

    override fun <RequestT, ResponseT, ErrorT> performRequestAsync(
        request: RequestT,
        endpoint: Endpoint<RequestT, ResponseT, ErrorT>,
        options: TransportOptions?,
    ): CompletableFuture<ResponseT> {
        val observation = observation(request, endpoint).start()
        // 직렬화 실패 등은 future 로 감싸지지 않고 바로 던져지므로 여기서도 관측을 닫는다.
        val future =
            try {
                observation.openScope().use { delegate.performRequestAsync(request, endpoint, options) }
            } catch (e: Throwable) {
                observation.error(e)
                observation.stop()
                throw e
            }
        return future.whenComplete { _, error ->
            error?.let { observation.error((it as? CompletionException)?.cause ?: it) }
            observation.stop()
        }
    }

    override fun close() = Unit

    private fun <RequestT> observation(
        request: RequestT,
        endpoint: Endpoint<RequestT, *, *>,
    ): Observation {
        val operation = operationName(request)
        return Observation
            .createNotStarted(NAME, registry)
            .contextualName("opensearch $operation")
            .lowCardinalityKeyValue("db.system", "opensearch")
            .lowCardinalityKeyValue("db.operation", operation)
            .highCardinalityKeyValue("db.namespace", indexName(endpoint.requestUrl(request)))
    }

    companion object {
        const val NAME = "opensearch.client.requests"

        fun operationName(request: Any?): String =
            request
                ?.javaClass
                ?.simpleName
                ?.removeSuffix("Request")
                ?.replaceFirstChar { it.lowercase() }
                ?.ifEmpty { null }
                ?: "unknown"

        /** `/place/_search`, `/place/_doc/1` 의 첫 구간. `_` 로 시작하면 인덱스가 아니라 API 경로다(`/_bulk`). */
        fun indexName(path: String): String =
            path
                .trimStart('/')
                .substringBefore('/')
                .takeUnless { it.isEmpty() || it.startsWith('_') }
                ?: "none"
    }
}
