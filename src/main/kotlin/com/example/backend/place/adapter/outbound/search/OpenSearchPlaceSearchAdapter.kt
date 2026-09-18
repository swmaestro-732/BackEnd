package com.example.backend.place.adapter.outbound.search

import com.example.backend.common.geo.Coordinate
import com.example.backend.place.application.port.outbound.PlaceMapBucket
import com.example.backend.place.application.port.outbound.PlaceMapHits
import com.example.backend.place.application.port.outbound.PlaceMapSearchPort
import com.example.backend.place.application.port.outbound.PlaceSearchCriteria
import com.example.backend.place.application.port.outbound.PlaceSearchHits
import com.example.backend.place.application.port.outbound.PlaceSearchQueryPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery
import org.opensearch.client.opensearch._types.query_dsl.FunctionBoostMode
import org.opensearch.client.opensearch._types.query_dsl.FunctionScoreMode
import org.opensearch.client.opensearch._types.query_dsl.Operator
import org.opensearch.client.opensearch._types.query_dsl.Query
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
) : PlaceSearchQueryPort,
    PlaceMapSearchPort {
    private val log = KotlinLogging.logger {}

    override fun search(criteria: PlaceSearchCriteria): PlaceSearchHits? {
        val client = clientProvider.ifAvailable ?: return null // endpoint 미설정 → DB 폴백

        return try {
            val response = client.search(buildRequest(criteria), Void::class.java)
            check(!response.timedOut() && response.shards().failed() == 0) { "검색이 일부만 완료되었습니다." }
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
            .query(scoredQuery(criteria))
            .apply {
                if (criteria.textTokens.isNotEmpty() || criteria.anchor != null ||
                    criteria.originalQuery.isNotBlank()
                ) {
                    sort { s -> s.score { sc -> sc.order(SortOrder.Desc) } }
                }
                sort { s -> s.doc { d -> d.order(SortOrder.Asc) } }
            }.build()

    /** 관련도 점수에 최대 1점의 거리 가산점을 더한다. 멀리 있는 결과를 필터링하지 않는다. */
    private fun scoredQuery(criteria: PlaceSearchCriteria): Query {
        val base = Query.of { q -> q.bool { b -> buildBool(b, criteria) } }
        if (criteria.anchor == null && criteria.originalQuery.isBlank()) return base
        return Query.of { q ->
            q.functionScore { fs ->
                fs.query(base).scoreMode(FunctionScoreMode.Sum).boostMode(FunctionBoostMode.Sum)
                if (criteria.originalQuery.isNotBlank()) {
                    fs.functions { f ->
                        f
                            .filter {
                                it.term { t ->
                                    t.field("name.keyword").value(FieldValue.of(criteria.originalQuery))
                                }
                            }.weight(1000.0)
                    }
                }
                criteria.anchor?.let { anchor ->
                    fs.functions { f ->
                        f
                            .gauss { g ->
                                g.field("location").placement { p ->
                                    p
                                        .origin(JsonData.of(mapOf("lat" to anchor.latitude, "lon" to anchor.longitude)))
                                        .scale(JsonData.of("2km"))
                                        .offset(JsonData.of("200m"))
                                        .decay(0.5)
                                }
                            }.weight(1.0)
                    }
                }
                fs
            }
        }
    }

    override fun searchMap(
        criteria: PlaceSearchCriteria,
        precision: Int,
    ): PlaceMapHits? {
        val client = clientProvider.ifAvailable ?: return null
        return try {
            val response =
                client.search(
                    SearchRequest
                        .Builder()
                        .index(INDEX_ALIAS)
                        .size(criteria.size)
                        .source { it.fetch(false) }
                        .trackTotalHits { it.enabled(true) }
                        .query { q -> q.bool { b -> buildBool(b, criteria) } }
                        .aggregations("grid") { a ->
                            a
                                .geohashGrid { g ->
                                    g
                                        .field("location")
                                        .precision { it.geohashLength(precision) }
                                        .size(256)
                                        .shardSize(256)
                                }.aggregations("center") { it.geoCentroid { c -> c.field("location") } }
                                .aggregations("sample") {
                                    it.topHits { t ->
                                        t.size(1).source { source -> source.fetch(false) }
                                    }
                                }
                        }.build(),
                    Void::class.java,
                )
            check(!response.timedOut() && response.shards().failed() == 0) { "지도 검색이 일부만 완료되었습니다." }
            val total = response.hits().total()!!.value()
            val buckets =
                response
                    .aggregations()["grid"]!!
                    .geohashGrid()
                    .buckets()
                    .array()
                    .map { bucket ->
                        val center =
                            bucket
                                .aggregations()["center"]!!
                                .geoCentroid()
                                .location()!!
                                .latlon()
                        PlaceMapBucket(
                            key = bucket.key(),
                            center = Coordinate(center.lat(), center.lon()),
                            count = bucket.docCount(),
                            singlePlaceId =
                                if (bucket.docCount() ==
                                    1L
                                ) {
                                    bucket
                                        .aggregations()["sample"]!!
                                        .topHits()
                                        .hits()
                                        .hits()
                                        .single()
                                        .id()!!
                                        .toLong()
                                } else {
                                    null
                                },
                        )
                    }.sortedBy { it.key }
            // 집계가 잘리면 일부 마커만 성공으로 반환하지 않고 DB에서 전체를 다시 집계한다.
            check(buckets.sumOf { it.count } == total) { "지도 집계가 전체 검색 결과를 포함하지 않습니다." }
            PlaceMapHits(total, response.hits().hits().map { it.id()!!.toLong() }, buckets)
        } catch (e: Exception) {
            log.warn { "지도 장소 검색 실패(DB 폴백): ${e.message}" }
            null
        }
    }

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
        criteria.viewport?.let { vp ->
            builder.filter { f ->
                f.geoBoundingBox { g ->
                    g.field("location").boundingBox { bb ->
                        bb.tlbr { t ->
                            t
                                .topLeft { l ->
                                    l.latlon { ll -> ll.lat(vp.northEast.latitude).lon(vp.southWest.longitude) }
                                }.bottomRight { l ->
                                    l.latlon { ll -> ll.lat(vp.southWest.latitude).lon(vp.northEast.longitude) }
                                }
                        }
                    }
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
