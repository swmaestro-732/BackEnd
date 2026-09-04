package com.example.backend.course.adapter.outbound.search

import com.example.backend.course.application.port.inbound.CourseSearchSort
import com.example.backend.course.application.port.outbound.CourseSearchCriteria
import com.example.backend.course.application.port.outbound.CourseSearchHit
import com.example.backend.course.application.port.outbound.CourseSearchPage
import com.example.backend.course.application.port.outbound.CourseSearchQueryPort
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.FieldValue
import org.opensearch.client.opensearch._types.SortOptions
import org.opensearch.client.opensearch._types.SortOrder
import org.opensearch.client.opensearch._types.query_dsl.Query
import org.opensearch.client.opensearch.core.SearchRequest
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * 아웃바운드 어댑터 — [CourseSearchQueryPort] 를 OpenSearch 검색으로 구현한다.
 *
 * [OpenSearchClient] 가 없으면(로컬·CI) 빈 페이지, 예외는 warn 로그만 남기고 빈 페이지를 돌려준다(fail-soft) —
 * 검색은 부가 기능이라 장애가 화면 전체를 500 으로 떨구지 않게 한다. alias `course` 를 대상으로 한다.
 *
 * 대상은 발행된 PUBLIC 코스로 고정하고(visibility·isPublished 필터), search_after 로 keyset 페이지네이션한다.
 * 정렬은 정렬 축 + 코스 id tiebreak 로 항상 유일해 커서 페이지가 안정적이다.
 */
@Component
class OpenSearchCourseSearchAdapter(
    private val clientProvider: ObjectProvider<OpenSearchClient>,
) : CourseSearchQueryPort {
    private val log = KotlinLogging.logger {}

    override fun search(criteria: CourseSearchCriteria): CourseSearchPage {
        val client = clientProvider.ifAvailable ?: return EMPTY // endpoint 미설정 → 빈 페이지
        return try {
            val request = buildRequest(criteria)
            val response = client.search(request, CourseDocument::class.java)
            val hits = response.hits().hits()
            val trimmed = hits.take(criteria.size)
            CourseSearchPage(
                hits = trimmed.mapNotNull { hit -> hit.source()?.toHit(hit.score()) },
                hasNext = hits.size > criteria.size,
            )
        } catch (e: Exception) {
            log.warn { "course 검색 실패(무시): ${e.message}" }
            EMPTY
        }
    }

    private fun buildRequest(criteria: CourseSearchCriteria): SearchRequest {
        val builder =
            SearchRequest
                .Builder()
                .index(INDEX_ALIAS)
                .size(criteria.size + 1) // 초과 1건으로 hasNext 판정
                .query { q -> q.bool { b -> b.must(keywordQuery(criteria.keyword)).filter(filters(criteria)) } }
                .sort(sortOptions(criteria.sort))
        criteria.searchAfter?.let { after -> builder.searchAfterVals(after.map(::toFieldValue)) }
        return builder.build()
    }

    /** keyword 가 비면 match_all, 있으면 title·description 다중 매치(nori 분석). */
    private fun keywordQuery(keyword: String?): Query =
        if (keyword.isNullOrBlank()) {
            Query.of { q -> q.matchAll { it } }
        } else {
            Query.of { q -> q.multiMatch { m -> m.query(keyword).fields("title", "description") } }
        }

    private fun filters(criteria: CourseSearchCriteria): List<Query> =
        buildList {
            add(termQuery("visibility", FieldValue.of("PUBLIC")))
            add(termQuery("isPublished", FieldValue.of(true)))
            criteria.area?.let { add(termQuery("area", FieldValue.of(it))) }
            criteria.category?.let { add(termQuery("category", FieldValue.of(it))) }
            if (criteria.tags.isNotEmpty()) {
                add(
                    Query.of { q ->
                        q.terms { t ->
                            t.field("tags").terms { ts -> ts.value(criteria.tags.map { FieldValue.of(it) }) }
                        }
                    },
                )
            }
        }

    private fun termQuery(
        field: String,
        value: FieldValue,
    ): Query = Query.of { q -> q.term { t -> t.field(field).value(value) } }

    /** 정렬 축 + id tiebreak. search_after 값 순서(primary, id)와 정확히 일치해야 한다. */
    private fun sortOptions(sort: CourseSearchSort): List<SortOptions> {
        val idSort = SortOptions.of { s -> s.field { f -> f.field("id").order(SortOrder.Desc) } }
        val primary =
            when (sort) {
                CourseSearchSort.RELEVANCE -> {
                    SortOptions.of { s -> s.score { sc -> sc.order(SortOrder.Desc) } }
                }

                CourseSearchSort.LATEST -> {
                    SortOptions.of { s -> s.field { f -> f.field("createdAt").order(SortOrder.Desc) } }
                }

                CourseSearchSort.POPULAR -> {
                    SortOptions.of { s -> s.field { f -> f.field("savesCnt").order(SortOrder.Desc) } }
                }
            }
        return listOf(primary, idSort)
    }

    private fun toFieldValue(value: Any): FieldValue =
        when (value) {
            is Double -> FieldValue.of(value)
            is Long -> FieldValue.of(value)
            else -> throw IllegalArgumentException("지원하지 않는 search_after 값 타입: ${value::class}")
        }

    private fun CourseDocument.toHit(score: Double?): CourseSearchHit =
        CourseSearchHit(
            id = id,
            authorId = userId.toLong(),
            title = title,
            coverImageUrl = coverImageUrl,
            theme = category,
            area = area,
            likesCnt = likesCnt,
            savesCnt = savesCnt,
            createdAt = Instant.ofEpochMilli(createdAt ?: 0L),
            score = score,
        )

    private companion object {
        const val INDEX_ALIAS = "course"
        val EMPTY = CourseSearchPage(hits = emptyList(), hasNext = false)
    }
}
