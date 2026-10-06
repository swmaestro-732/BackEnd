package com.example.backend.bootstrap.search

import com.example.backend.common.search.OpenSearchProperties
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.indices.CreateIndexRequest
import org.opensearch.client.opensearch.indices.CreateIndexResponse
import org.opensearch.client.opensearch.indices.ExistsAliasRequest
import org.opensearch.client.opensearch.indices.ExistsRequest
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient
import org.opensearch.client.opensearch.indices.PutAliasRequest
import org.opensearch.client.opensearch.indices.PutAliasResponse
import org.opensearch.client.opensearch.indices.PutMappingRequest
import org.opensearch.client.opensearch.indices.PutMappingResponse
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.endpoints.BooleanResponse
import org.opensearch.client.util.ObjectBuilder
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.DefaultApplicationArguments
import java.util.function.Function

class OpenSearchIndexInitializerTest {
    private val clientProvider = mock(ObjectProvider::class.java) as ObjectProvider<OpenSearchClient>
    private val initializer = OpenSearchIndexInitializer(clientProvider, OpenSearchProperties())
    private val args = DefaultApplicationArguments()

    /** Kotlin 에서 오버로드(Function 인자)를 지목하기 위한 typed any 매처. */
    private fun <T> anyArg(): T = ArgumentMatchers.any()

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
        // client._transport() 가 null → loadDefinition 진입 즉시 NPE → forEach catch → warn → 전파 없음

        initializer.run(args) // no exception
    }

    @Test
    fun `인덱스가 없으면 매핑 리소스를 파싱해 place, course 를 settings 와 함께 생성하고 alias 를 붙인다`() {
        val transport = mock(OpenSearchTransport::class.java)
        `when`(transport.jsonpMapper()).thenReturn(JacksonJsonpMapper()) // loadDefinition 이 실제 JSON 파싱을 수행
        val client = mock(OpenSearchClient::class.java)
        `when`(client._transport()).thenReturn(transport)
        val indices = mock(OpenSearchIndicesClient::class.java)
        `when`(client.indices()).thenReturn(indices)
        `when`(clientProvider.ifAvailable).thenReturn(client)

        // 빌더 람다를 실제로 실행해(코드 라인 커버) 인덱스 미존재 → 생성 경로를 태운다.
        `when`(indices.exists(anyArg<Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>>()))
            .thenAnswer { inv ->
                (
                    inv.getArgument(
                        0,
                    ) as Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>
                ).apply(ExistsRequest.Builder())
                BooleanResponse(false)
            }
        `when`(indices.create(anyArg<Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>>>()))
            .thenAnswer { inv ->
                (
                    inv.getArgument(
                        0,
                    ) as Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>>
                ).apply(
                    CreateIndexRequest.Builder(),
                )
                mock(CreateIndexResponse::class.java)
            }
        `when`(indices.putMapping(anyArg<Function<PutMappingRequest.Builder, ObjectBuilder<PutMappingRequest>>>()))
            .thenAnswer { inv ->
                (
                    inv.getArgument(
                        0,
                    ) as Function<PutMappingRequest.Builder, ObjectBuilder<PutMappingRequest>>
                ).apply(
                    PutMappingRequest.Builder(),
                )
                mock(PutMappingResponse::class.java)
            }
        `when`(indices.existsAlias(anyArg<Function<ExistsAliasRequest.Builder, ObjectBuilder<ExistsAliasRequest>>>()))
            .thenAnswer { inv ->
                (
                    inv.getArgument(
                        0,
                    ) as Function<ExistsAliasRequest.Builder, ObjectBuilder<ExistsAliasRequest>>
                ).apply(
                    ExistsAliasRequest.Builder(),
                )
                BooleanResponse(false)
            }
        `when`(indices.putAlias(anyArg<Function<PutAliasRequest.Builder, ObjectBuilder<PutAliasRequest>>>()))
            .thenAnswer { inv ->
                (
                    inv.getArgument(
                        0,
                    ) as Function<PutAliasRequest.Builder, ObjectBuilder<PutAliasRequest>>
                ).apply(PutAliasRequest.Builder())
                mock(PutAliasResponse::class.java)
            }

        initializer.run(args)

        // place, course 두 인덱스 모두 생성 + alias 부착됐는지 검증(리소스 파싱이 성공했다는 뜻이기도 하다).
        verify(
            indices,
            times(2),
        ).create(anyArg<Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>>>())
        verify(indices, times(2)).putAlias(anyArg<Function<PutAliasRequest.Builder, ObjectBuilder<PutAliasRequest>>>())
    }

    @Test
    fun `인덱스가 이미 있으면 생성하지 않고 매핑만 동기화한다`() {
        val transport = mock(OpenSearchTransport::class.java)
        `when`(transport.jsonpMapper()).thenReturn(JacksonJsonpMapper())
        val client = mock(OpenSearchClient::class.java)
        `when`(client._transport()).thenReturn(transport)
        val indices = mock(OpenSearchIndicesClient::class.java)
        `when`(client.indices()).thenReturn(indices)
        `when`(clientProvider.ifAvailable).thenReturn(client)

        `when`(indices.exists(anyArg<Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>>()))
            .thenReturn(BooleanResponse(true)) // 이미 존재 → create 스킵
        `when`(indices.putMapping(anyArg<Function<PutMappingRequest.Builder, ObjectBuilder<PutMappingRequest>>>()))
            .thenAnswer { inv ->
                (
                    inv.getArgument(
                        0,
                    ) as Function<PutMappingRequest.Builder, ObjectBuilder<PutMappingRequest>>
                ).apply(
                    PutMappingRequest.Builder(),
                )
                mock(PutMappingResponse::class.java)
            }
        `when`(indices.existsAlias(anyArg<Function<ExistsAliasRequest.Builder, ObjectBuilder<ExistsAliasRequest>>>()))
            .thenReturn(BooleanResponse(true)) // alias 도 존재 → putAlias 스킵

        initializer.run(args)

        verify(
            indices,
            never(),
        ).create(anyArg<Function<CreateIndexRequest.Builder, ObjectBuilder<CreateIndexRequest>>>())
        verify(indices, never()).putAlias(anyArg<Function<PutAliasRequest.Builder, ObjectBuilder<PutAliasRequest>>>())
        verify(
            indices,
            times(2),
        ).putMapping(anyArg<Function<PutMappingRequest.Builder, ObjectBuilder<PutMappingRequest>>>())
    }
}
