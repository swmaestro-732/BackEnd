package com.example.backend.course.adapter.outbound.search

import com.example.backend.bootstrap.config.CourseRecommendProperties
import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.course.application.port.outbound.CourseRecommendCriteria
import com.example.backend.course.application.port.outbound.CourseRecommendHit
import com.example.backend.course.application.port.outbound.CourseRecommendQueryPort
import com.example.backend.course.application.port.outbound.CourseRecommendResultPage
import com.example.backend.course.application.port.outbound.CourseRegionFilter
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.FieldValueFactorModifier
import org.opensearch.client.opensearch._types.query_dsl.FunctionBoostMode
import org.opensearch.client.opensearch._types.query_dsl.FunctionScore
import org.opensearch.client.opensearch._types.query_dsl.FunctionScoreMode
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [CourseRecommendQueryPort] 를 OpenSearch function_score 로 구현한다.
 *
 * 점수 = 관심 일치(bool.should 의 constant_score: 테마·시군구·읍면동) + 최신성(createdAt exp 감쇠) + log1p(savesCnt) + log1p(likesCnt).
 * 관심 일치는 함수가 아니라 base 쿼리 점수로 둔다 — 클라이언트 라이브러리가 weight 만 있는 함수를 만들 수 없고,
 * boost_mode=sum 이면 결과는 같다(`_explain` 에서 항별 기여가 그대로 보인다). should 는 선택이라 못 맞춰도 탈락하지 않는다.
 * 정렬은 `_score desc, id desc` 로 고정하고 search_after 로 페이지를 넘긴다. 감쇠 origin 은 호출자가 고정해 넘긴다.
 * 클라이언트가 없거나(로컬·CI) 실패하면 null(fail-soft) — 호출자가 DB 폴백한다.
 */
@Component
class OpenSearchCourseRecommendAdapter(
    private val clientProvider: ObjectProvider<OpenSearchClient>,
    properties: OpenSearchProperties,
    private val weights: CourseRecommendProperties,
) : CourseRecommendQueryPort {
    private val log = KotlinLogging.logger {}
    private val indexAlias = properties.withPrefix("course")

    override fun recommend(criteria: CourseRecommendCriteria): CourseRecommendResultPage? {
        val client = clientProvider.ifAvailable ?: return null // endpoint 미설정 → 폴백
        return try {
            val response = client.search(buildRequest(criteria), CourseDocument::class.java)
            val allHits = response.hits().hits()
            val hasNext = allHits.size > criteria.size
            CourseRecommendResultPage(
                hits =
                    allHits.take(criteria.size).mapNotNull { hit ->
                        hit.source()?.let { CourseRecommendHit(row = it.toRow(), score = hit.score() ?: 0.0) }
                    },
                hasNext = hasNext,
            )
        } catch (e: Exception) {
            log.warn { "course 추천 조회 실패(DB 폴백): ${e.message}" }
            null
        }
    }

    private fun buildRequest(criteria: CourseRecommendCriteria): SearchRequest {
        val builder =
            SearchRequest
                .Builder()
                .index(indexAlias)
                .size(criteria.size + 1) // 초과 1건으로 hasNext 판정
                .query { q ->
                    q.functionScore { fs ->
                        fs
                            .query(baseQuery(criteria))
                            .functions(scoreFunctions(criteria))
                            .scoreMode(FunctionScoreMode.Sum)
                            .boostMode(FunctionBoostMode.Sum)
                    }
                }.sort(
                    listOf(
                        SortOptions.of { s -> s.score { sc -> sc.order(SortOrder.Desc) } },
                        SortOptions.of { s -> s.field { f -> f.field("id").order(SortOrder.Desc) } },
                    ),
                )
        criteria.after?.let { builder.searchAfterVals(listOf(FieldValue.of(it.score), FieldValue.of(it.id))) }
        return builder.build()
    }

    /** 고정 필터(발행 PUBLIC·재색인 완료분·선택 카테고리·선택 지역) + 관심 일치 가산 should. */
    private fun baseQuery(criteria: CourseRecommendCriteria): Query =
        Query.of { q ->
            q.bool { b ->
                b.filter(
                    buildList {
                        add(termQuery("visibility", FieldValue.of("PUBLIC")))
                        add(termQuery("isPublished", FieldValue.of(true)))
                        // 미백필 문서는 정렬 tiebreak(id)가 없어 search_after 가 어긋난다 → 재색인 완료 문서만.
                        add(Query.of { e -> e.exists { x -> x.field("id") } })
                        criteria.category?.let { add(termQuery("category", FieldValue.of(it))) }
                        criteria.region?.let { add(regionQuery(it)) }
                    },
                )
                val interests =
                    buildList {
                        if (criteria.likeThemes.isNotEmpty()) {
                            add(
                                constantScore("category", criteria.likeThemes, weights.themeWeight),
                            )
                        }
                        if (criteria.sigunguCodes.isNotEmpty()) {
                            add(constantScore("sigunguCode", criteria.sigunguCodes, weights.sigunguWeight))
                        }
                        if (criteria.dongCodes.isNotEmpty()) {
                            add(
                                constantScore("areaCode", criteria.dongCodes, weights.dongWeight),
                            )
                        }
                    }
                if (interests.isNotEmpty()) b.should(interests)
                b
            }
        }

    /** 최신성 감쇠·저장수·좋아요수 — 각각 weight 를 곱해 더한다. */
    private fun scoreFunctions(criteria: CourseRecommendCriteria): List<FunctionScore> =
        listOf(
            FunctionScore.of { f ->
                f.weight(weights.recencyWeight).exp { d ->
                    d.field("createdAt").placement { p ->
                        p
                            .origin(JsonData.of(criteria.origin.toEpochMilli()))
                            .scale(JsonData.of(weights.recencyScale))
                            .offset(JsonData.of(weights.recencyOffset))
                            .decay(weights.recencyDecay)
                    }
                }
            },
            FunctionScore.of { f ->
                f.weight(weights.savesWeight).fieldValueFactor { v ->
                    v.field("savesCnt").modifier(FieldValueFactorModifier.Log1p).missing(0.0)
                }
            },
            FunctionScore.of { f ->
                f.weight(weights.likesWeight).fieldValueFactor { v ->
                    v.field("likesCnt").modifier(FieldValueFactorModifier.Log1p).missing(0.0)
                }
            },
        )

    /** terms 일치 시 boost 만큼 점수를 주는 constant_score. 값이 여러 개 맞아도 한 번만 더해진다. */
    private fun constantScore(
        field: String,
        values: List<String>,
        boost: Double,
    ): Query =
        Query.of { q ->
            q.constantScore { cs ->
                cs
                    .filter { f ->
                        f.terms { t ->
                            t.field(field).terms { ts -> ts.value(values.map { FieldValue.of(it) }) }
                        }
                    }.boost(boost.toFloat())
            }
        }

    private fun termQuery(
        field: String,
        value: FieldValue,
    ): Query = Query.of { q -> q.term { t -> t.field(field).value(value) } }

    /** areaCode 가 include prefix 중 하나로 시작하고 exclude prefix 로는 시작하지 않는 문서만. */
    private fun regionQuery(region: CourseRegionFilter): Query =
        Query.of { q ->
            q.bool { b ->
                region.includePrefixes.forEach { prefix ->
                    b.should { s -> s.prefix { p -> p.field("areaCode").value(prefix) } }
                }
                region.excludePrefixes.forEach { prefix ->
                    b.mustNot { m -> m.prefix { p -> p.field("areaCode").value(prefix) } }
                }
                b.minimumShouldMatch("1")
            }
        }
}
