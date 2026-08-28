package com.example.backend.place.adapter.outbound.search

import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.application.port.outbound.PlaceSearchHits
import com.example.backend.place.application.port.outbound.PlaceSearchQueryPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.Operator
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [PlaceSearchQueryPort] 를 OpenSearch 검색으로 구현한다.
 *
 * [OpenSearchClient] 가 없거나(=opensearch.endpoint 미주입, 로컬·CI) 검색이 실패하면 null 을 돌려
 * 호출부가 DB LIKE 로 폴백하게 한다(fail-soft). alias `place` 를 조회하며, id 만 필요하므로 _source 는 내리지 않는다.
 *
 * 정렬: 텍스트 토큰이 있으면 관련도(_score) 내림차순 + _doc 타이브레이크, 필터-only 브라우즈면 _doc.
 * (_id 정렬은 fielddata 요구로 피한다. _doc 은 페이지 간 순서 결정성만 보장하는 근사 — 최신순 정렬은
 * 색인에 숫자 id·시각 필드를 추가하는 다음 리인덱스 때 후속.)
 */
@Component
class OpenSearchPlaceSearchAdapter(
    private val clientProvider: ObjectProvider<OpenSearchClient>,
) : PlaceSearchQueryPort {
    private val log = KotlinLogging.logger {}

    override fun search(criteria: PlaceSearchCriteria): PlaceSearchHits? {
        val client = clientProvider.ifAvailable ?: return null // endpoint 미설정 → DB 폴백

        return try {
            val response = client.search(buildRequest(criteria), Void::class.java)
            PlaceSearchHits(
                ids = response.hits().hits().mapNotNull { it.id()?.toLongOrNull() },
                totalCount = response.hits().total()?.value() ?: 0L,
            )
        } catch (e: Exception) {
            log.warn { "place 검색 실패(DB 폴백): ${e.message}" }
            null
        }
    }

    private fun buildRequest(criteria: PlaceSearchCriteria): SearchRequest =
        SearchRequest
            .Builder()
            .index(INDEX_ALIAS)
            .from(criteria.from)
            .size(criteria.size)
            // totalCount("장소 N곳")·hasNext 판정에 정확한 전체 건수가 필요해 기본 10,000 상한을 푼다.
            .trackTotalHits { t -> t.enabled(true) }
            .source { s -> s.fetch(false) }
            .query { q -> q.bool { b -> buildBool(b, criteria) } }
            .apply {
                if (criteria.textTokens.isNotEmpty()) {
                    sort { s -> s.score { sc -> sc.order(SortOrder.Desc) } }
                }
                sort { s -> s.doc { d -> d.order(SortOrder.Asc) } }
            }.build()

    private fun buildBool(
        builder: BoolQuery.Builder,
        criteria: PlaceSearchCriteria,
    ): BoolQuery.Builder {
        builder.filter { f -> f.term { t -> t.field("status").value(FieldValue.of(STATUS_ACTIVE)) } }
        if (criteria.categories.isNotEmpty()) {
            builder.filter { f ->
                f.terms { t ->
                    t.field("category").terms { v -> v.value(criteria.categories.map { FieldValue.of(it.name) }) }
                }
            }
        }
        if (criteria.areaCodePrefixes.isNotEmpty()) {
            builder.filter { f ->
                f.bool { areas ->
                    criteria.areaCodePrefixes.forEach { prefix ->
                        areas.should { s -> s.prefix { p -> p.field("areaCode").value(prefix) } }
                    }
                    areas.minimumShouldMatch("1")
                }
            }
        }
        if (criteria.textTokens.isNotEmpty()) {
            builder.must { m ->
                m.multiMatch { mm ->
                    mm
                        .query(criteria.textTokens.joinToString(" "))
                        .fields("name^3", "address", "description")
                        .operator(Operator.And)
                }
            }
        }
        return builder
    }

    private companion object {
        const val INDEX_ALIAS = "place"
        const val STATUS_ACTIVE = "ACTIVE"
    }
}
