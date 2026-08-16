package com.example.backend.bootstrap.search

import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

    private data class IndexDef(
        val alias: String,
        val index: String,
        val mappingResource: String,
    )

    private val indices =
        listOf(
            IndexDef(alias = "place", index = "place_v1", mappingResource = "opensearch/place.json"),
            IndexDef(alias = "course", index = "course_v1", mappingResource = "opensearch/course.json"),
        )

    override fun run(args: ApplicationArguments) {
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정(로컬/CI) → no-op

        indices.forEach { def ->
            try {
                // alias 가 이미 있으면(이전 부팅·배포에서 생성) 아무것도 안 한다 — 멱등.
                if (client.indices().existsAlias { it.name(def.alias) }.value()) return@forEach

                // 인덱스 존재 여부와 alias 부착을 분리한다 — 이전 부팅이 create 후 putAlias 전에 죽었더라도(인덱스는 있고
                // alias 만 없는 상태) 다음 부팅에서 alias 를 복구하도록. (인덱스가 있는데 다시 create 하면 예외가 나 alias 가 영구 미생성됨)
                if (!client.indices().exists { it.index(def.index) }.value()) {
                    // 매핑 JSON(TypeMapping 본문 {properties:...})을 클라이언트 매퍼로 역직렬화해 인덱스를 만든다.
                    val mapper = client._transport().jsonpMapper()
                    val mapping =
                        ClassPathResource(def.mappingResource).inputStream.use { input ->
                            mapper.jsonProvider().createParser(input).use { parser ->
                                TypeMapping._DESERIALIZER.deserialize(parser, mapper)
                            }
                        }
                    client.indices().create { c -> c.index(def.index).mappings(mapping) }
                }
                client.indices().putAlias { p -> p.index(def.index).name(def.alias) }
                log.info("OpenSearch 인덱스 준비: {} (alias {})", def.index, def.alias)
            } catch (e: Exception) {
                log.warn("OpenSearch 인덱스 초기화 실패(무시): {} — {}", def.index, e.message)
            }
        }
    }
}
