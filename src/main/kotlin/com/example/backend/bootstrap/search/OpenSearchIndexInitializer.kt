package com.example.backend.bootstrap.search

import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

/**
 * 부팅 시 OpenSearch 인덱스를 매핑 JSON 으로 생성한다(create-if-not-exists + alias).
 *
 * [OpenSearchClient] 가 있을 때만(=opensearch.endpoint 주입 시) 동작하고, 로컬·CI(엔드포인트 미주입)에서는
 * no-op 이다. 인덱스 생성 실패는 warn 로그만 남기고 부팅을 막지 않는다(fail-soft — health UNKNOWN 철학과 동일).
 *
 * 실제 인덱스는 버전 접미사(`place_v1`)로 만들고 alias(`place`)를 붙인다 — 매핑 변경 시 `_v2` 를 만들어
 * reindex 후 alias 를 원자적으로 스위칭하기 위함이다(불변 매핑 대응, 런북은 .ai/backend/search.md).
 */
@Component
@Order(0)
class OpenSearchIndexInitializer(
    private val clientProvider: ObjectProvider<OpenSearchClient>,
) : ApplicationRunner {
    private val log = KotlinLogging.logger {}

    private data class IndexDef(
        val alias: String,
        val index: String,
        val mappingResource: String,
    )

    private val indices =
        listOf(
            IndexDef(alias = "place", index = "place_v1", mappingResource = "opensearch/place.json"),
            // course 검색 필터/정렬용 필드(id·category·tags·coverImageUrl)는 기존 매핑에 필드를 '추가'만 하므로
            // 새 인덱스(_v2)+alias 스위치 없이 같은 인덱스에 가산적 putMapping 으로 반영한다(재색인·alias 전환 불필요).
            IndexDef(alias = "course", index = "course_v1", mappingResource = "opensearch/course.json"),
        )

    override fun run(args: ApplicationArguments) {
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정(로컬/CI) → no-op

        indices.forEach { def ->
            try {
                val mapping = loadMapping(client, def.mappingResource)

                // 인덱스가 없으면 전체 매핑으로 생성한다. 이전 부팅이 create 후 putAlias 전에 죽어 인덱스만 있고
                // alias 가 없는 경우도 아래 putAlias 로 복구된다(인덱스가 있는데 다시 create 하면 예외가 나 alias 가 영구 미생성됨).
                if (!client.indices().exists { it.index(def.index) }.value()) {
                    client.indices().create { c -> c.index(def.index).mappings(mapping) }
                }
                // 이미 있는 인덱스에도 매핑을 동기화한다 — 새로 추가된 검색 필드를 반영(가산적 putMapping, 기존 필드 동일 정의는 no-op).
                // 이 덕분에 이전 배포로 alias 가 이미 존재하는 환경에서도 새 필드가 인덱스에 반영돼 검색이 동작한다.
                // (기존 문서는 새 필드가 비어 있으니 CourseReindexService 재색인으로 backfill — 런북 .ai/backend/search.md.)
                client.indices().putMapping { p -> p.index(def.index).properties(mapping.properties()) }
                if (!client.indices().existsAlias { it.name(def.alias) }.value()) {
                    client.indices().putAlias { p -> p.index(def.index).name(def.alias) }
                }
                log.info { "OpenSearch 인덱스 준비: ${def.index} (alias ${def.alias})" }
            } catch (e: Exception) {
                log.warn { "OpenSearch 인덱스 초기화 실패(무시): ${def.index} — ${e.message}" }
            }
        }
    }

    /** 매핑 JSON(TypeMapping 본문 {properties:...})을 클라이언트 매퍼로 역직렬화한다. */
    private fun loadMapping(
        client: OpenSearchClient,
        resource: String,
    ): TypeMapping {
        val mapper = client._transport().jsonpMapper()
        return ClassPathResource(resource).inputStream.use { input ->
            mapper.jsonProvider().createParser(input).use { parser ->
                TypeMapping._DESERIALIZER.deserialize(parser, mapper)
            }
        }
    }
}
