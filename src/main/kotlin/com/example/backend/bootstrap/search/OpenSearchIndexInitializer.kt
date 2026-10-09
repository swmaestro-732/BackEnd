package com.example.backend.bootstrap.search

import com.example.backend.bootstrap.config.OpenSearchProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.mapping.TypeMapping
import org.opensearch.client.opensearch.indices.IndexSettings
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.StringReader

/**
 * 부팅 시 OpenSearch 인덱스를 설정(analysis)·매핑 JSON 으로 생성한다(create-if-not-exists + alias).
 *
 * [OpenSearchClient] 가 있을 때만(=opensearch.endpoint 주입 시) 동작하고, 로컬·CI(엔드포인트 미주입)에서는
 * no-op 이다. 인덱스 생성 실패는 warn 로그만 남기고 부팅을 막지 않는다(fail-soft — health UNKNOWN 철학과 동일).
 *
 * 실제 인덱스는 버전 접미사(`place_v2`)로 만들고 alias(`place`)를 붙인다 — analyzer·매핑 변경 시 `_v3` 를 만들어
 * reindex 후 alias 를 원자적으로 스위칭하기 위함이다(불변 매핑 대응, 런북은 .ai/backend/search.md).
 *
 * v2 = 한글 형태소 검색 정확도(복합어)용 커스텀 `korean` analyzer 도입(SCRUM-552, nori_tokenizer decompound_mode=mixed
 * + 사용자 사전). analyzer 는 인덱스 settings 라 기존 인덱스에 putMapping 으로 못 바꾸므로 새 버전 인덱스로 만든다.
 * 신규 환경·CI 는 v2 를 바로 만들고, 기존 alias(v1)가 있는 환경은 v2 backfill 후 런북대로 alias 를 스위칭한다.
 */
@Component
@Order(0)
class OpenSearchIndexInitializer(
    private val clientProvider: ObjectProvider<OpenSearchClient>,
    private val properties: OpenSearchProperties,
) : ApplicationRunner {
    private val log = KotlinLogging.logger {}
    private val objectMapper = ObjectMapper()

    private data class IndexDef(
        val alias: String,
        val index: String,
        val mappingResource: String,
    )

    private val indices =
        listOf(
            IndexDef(
                alias = properties.withPrefix("place"),
                index = properties.withPrefix("place_v2"),
                mappingResource = "opensearch/place.json",
            ),
            IndexDef(
                alias = properties.withPrefix("course"),
                index = properties.withPrefix("course_v2"),
                mappingResource = "opensearch/course.json",
            ),
        )

    override fun run(args: ApplicationArguments) {
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정(로컬/CI) → no-op

        indices.forEach { def ->
            try {
                val definition = loadDefinition(client, def.mappingResource)

                // 인덱스가 없으면 settings(analyzer)+매핑으로 생성한다. 이전 부팅이 create 후 putAlias 전에 죽어 인덱스만 있고
                // alias 가 없는 경우도 아래 putAlias 로 복구된다(인덱스가 있는데 다시 create 하면 예외가 나 alias 가 영구 미생성됨).
                if (!client.indices().exists { it.index(def.index) }.value()) {
                    // 동시 부팅 경합: 다른 인스턴스가 exists 이후~create 사이에 먼저 만들면 "이미 존재" 오류가 난다.
                    // 그 경우 무시하고 아래 putMapping·putAlias 복구를 계속한다(안 그러면 alias 가 영구 미생성될 수 있다).
                    runCatching {
                        client.indices().create { c ->
                            c.index(def.index).settings(definition.settings).mappings(definition.mappings)
                        }
                    }.onFailure { log.warn { "인덱스 생성 경합(이미 존재 가능) 무시하고 계속: ${def.index} — ${it.message}" } }
                }
                // 이미 있는 인덱스에도 매핑을 동기화한다 — 새로 추가된 검색 필드를 반영(가산적 putMapping, 기존 필드 동일 정의는 no-op).
                // analyzer(settings)는 생성 시에만 적용되며 putMapping 으로 바꿀 수 없다 — 그래서 변경 시 새 버전 인덱스를 쓴다.
                client.indices().putMapping { p -> p.index(def.index).properties(definition.mappings.properties()) }
                // alias 조회를 이 인덱스로 좁힌다 — 이름만 주면 클러스터 전체 alias 를 보므로, dev 처럼 `dev-*` 권한만 가진
                // 계정은 security_exception 으로 막혀 alias 가 영구 미생성된다(공유 도메인 격리, SCRUM-567).
                if (!client.indices().existsAlias { it.index(def.index).name(def.alias) }.value()) {
                    client.indices().putAlias { p -> p.index(def.index).name(def.alias) }
                }
                log.info { "OpenSearch 인덱스 준비: ${def.index} (alias ${def.alias})" }
            } catch (e: Exception) {
                log.error(e) { "OpenSearch 인덱스 초기화 실패(무시): ${def.index} — ${e.message}" }
            }
        }
    }

    private data class IndexDefinition(
        val settings: IndexSettings,
        val mappings: TypeMapping,
    )

    /**
     * 인덱스 정의 JSON({settings, mappings})을 settings·mappings 로 나눠 역직렬화한다.
     * opensearch-java 클라이언트는 create 본문 전체(withJson)·createReader 를 미지원해, Jackson 으로 트리를 읽어
     * 하위 오브젝트(settings/mappings)를 각각 클라이언트 매퍼(createParser)로 역직렬화한다.
     */
    private fun loadDefinition(
        client: OpenSearchClient,
        resource: String,
    ): IndexDefinition {
        val mapper = client._transport().jsonpMapper()
        val root = ClassPathResource(resource).inputStream.use { objectMapper.readTree(it) }
        val settings =
            mapper
                .jsonProvider()
                .createParser(
                    StringReader(objectMapper.writeValueAsString(root.get("settings"))),
                ).use {
                    IndexSettings._DESERIALIZER.deserialize(it, mapper)
                }
        val mappings =
            mapper
                .jsonProvider()
                .createParser(
                    StringReader(objectMapper.writeValueAsString(root.get("mappings"))),
                ).use {
                    TypeMapping._DESERIALIZER.deserialize(it, mapper)
                }
        return IndexDefinition(settings, mappings)
    }
}
