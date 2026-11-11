package com.example.backend.course.adapter.outbound.search

import com.example.backend.bootstrap.config.OpenSearchConfig
import com.example.backend.bootstrap.config.OpenSearchProperties
import com.example.backend.course.application.event.CourseCounter
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import com.example.backend.course.domain.model.Course
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.json.JsonData
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch._types.OpenSearchException
import org.opensearch.client.opensearch._types.Script
import org.opensearch.client.opensearch.core.bulk.BulkOperation
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [CourseSearchIndexPort] 를 OpenSearch 색인으로 구현한다.
 *
 * [OpenSearchClient] 가 없으면(=opensearch.endpoint 미주입, 로컬·CI) no-op, 예외는 warn 로그만 남기고
 * 삼킨다(fail-soft) — 검색은 부가 기능이라 코스 쓰기 흐름을 막지 않는다. alias `course` 에 upsert/delete 한다.
 */
@Component
class OpenSearchCourseIndexAdapter(
    @Qualifier(OpenSearchConfig.INDEX_CLIENT) private val clientProvider: ObjectProvider<OpenSearchClient>,
    properties: OpenSearchProperties,
) : CourseSearchIndexPort {
    private val log = KotlinLogging.logger {}
    private val indexAlias = properties.withPrefix("course")

    override fun save(course: Course) {
        val id = course.id ?: return
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op
        try {
            client.index { req ->
                req
                    .index(indexAlias)
                    .requireAlias(true)
                    .id(id.toString())
                    .document(course.toDocument())
            }
        } catch (e: Exception) {
            log.error(e) { "course 색인 실패(무시): id=$id — ${e.message}" }
        }
    }

    override fun save(courses: List<Course>) {
        if (courses.isEmpty()) return
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op
        try {
            val documents = courses.filter { it.id != null }
            if (documents.isEmpty()) return
            val response =
                client.bulk { bulk ->
                    // alias 가 없을 때 같은 이름의 인덱스가 동적 매핑으로 자동 생성되지 않게 한다(실패시키고 재색인으로 채움)
                    bulk.requireAlias(true)
                    bulk.operations(
                        documents.map { course ->
                            val document = course.toDocument()
                            BulkOperation
                                .Builder()
                                .index { op -> op.index(indexAlias).id(course.id.toString()).document(document) }
                                .build()
                        },
                    )
                }
            // bulk 는 예외 없이 200 을 주면서 개별 문서만 거부될 수 있다(매핑 충돌 등) → 항목별 실패를 집계해 로그로 드러낸다.
            if (response.errors()) {
                val failed = response.items().count { it.error() != null }
                log.error { "course 색인 부분 실패(무시): $failed/${documents.size}건 실패" }
            }
        } catch (e: Exception) {
            log.error(e) { "course 색인 실패(무시): ${courses.size}건 — ${e.message}" }
        }
    }

    override fun delete(courseId: Long) {
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op
        try {
            client.delete { req -> req.index(indexAlias).id(courseId.toString()) }
        } catch (e: Exception) {
            log.error(e) { "course 색인 삭제 실패(무시): id=$courseId — ${e.message}" }
        }
    }

    override fun adjustCounter(
        courseIds: List<Long>,
        counter: CourseCounter,
        delta: Int,
    ) {
        if (delta == 0 || courseIds.isEmpty()) return
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op
        val field = counter.indexField()
        val script =
            Script.of { s ->
                s.inline { i ->
                    i
                        .lang("painless")
                        .source(ADJUST_COUNTER_SCRIPT)
                        .params("field", JsonData.of(field))
                        .params("delta", JsonData.of(delta))
                }
            }
        try {
            // 델타 가산 스크립트라 동시 이벤트의 도착 순서가 바뀌어도 같은 값으로 수렴한다(스냅샷 덮어쓰기와 달리 순서 역전에 안전).
            // 단 0 클램프가 걸린 경우(취소가 색인보다 먼저 닿음)는 수렴하지 않고 다음 전체 색인에서 복구된다.
            // 단건도 bulk 한 요청으로 보낸다 — 탈퇴 정리처럼 코스가 여러 개여도 OpenSearch 왕복은 한 번이다.
            val response =
                client.bulk { bulk ->
                    bulk.requireAlias(true)
                    bulk.operations(
                        courseIds.distinct().map { courseId ->
                            BulkOperation
                                .Builder()
                                .update<CourseDocument> { op ->
                                    op
                                        .index(indexAlias)
                                        .id(courseId.toString())
                                        .retryOnConflict(COUNTER_RETRY_ON_CONFLICT)
                                        .script(script)
                                }.build()
                        },
                    )
                }
            if (response.errors()) {
                // 아직 색인되지 않은 코스(미백필·색인 실패분)는 다음 전체 색인이 채운다 — 404 는 정상 경로로 본다.
                val failed = response.items().filter { it.error() != null && it.status() != NOT_FOUND_STATUS }
                if (failed.isNotEmpty()) {
                    val failedIds = failed.map { it.id() }
                    log.error {
                        "course $field 갱신 부분 실패(무시): ${failed.size}/${courseIds.size}건 delta=$delta ids=$failedIds"
                    }
                } else {
                    log.debug { "course $field 갱신 대상 문서 없음(무시): ids=$courseIds" }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "course $field 갱신 실패(무시): ids=$courseIds delta=$delta — ${e.message}" }
        }
    }

    private fun CourseCounter.indexField(): String =
        when (this) {
            CourseCounter.SAVES -> "savesCnt"
            CourseCounter.LIKES -> "likesCnt"
        }

    override fun deleteByAuthor(authorId: Long) {
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op
        try {
            client.deleteByQuery { req ->
                req.index(indexAlias).query { q ->
                    q.term { t -> t.field("userId").value { v -> v.stringValue(authorId.toString()) } }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "course 작성자 색인 삭제 실패(무시): authorId=$authorId — ${e.message}" }
        }
    }

    private fun Course.toDocument(): CourseDocument =
        CourseDocument(
            // 색인 대상은 id 가 채워진(영속화된) 코스뿐이다 — 호출부(save)에서 null 을 걸러낸다.
            id = id!!,
            title = title,
            description = description,
            area = area,
            areaCode = areaCode,
            sigunguCode = areaCode?.take(SIGUNGU_CODE_LENGTH),
            category = categories.map { it.name },
            tags = tags,
            coverImageUrl = coverImageUrl,
            visibility = visibility.name,
            isPublished = isPublished,
            userId = userId.toString(),
            likesCnt = likesCnt,
            savesCnt = savesCnt,
            createdAt = createdAt?.toEpochMilliseconds(),
        )

    private companion object {
        const val SIGUNGU_CODE_LENGTH = 5
        const val COUNTER_RETRY_ON_CONFLICT = 3
        const val NOT_FOUND_STATUS = 404

        /** 색인 문서의 params.field 카운터에 params.delta 를 더한다(음수 방지). 구 문서에 필드가 없으면 0 으로 본다. */
        const val ADJUST_COUNTER_SCRIPT =
            "int current = ctx._source[params.field] == null ? 0 : (int) ctx._source[params.field]; " +
                "ctx._source[params.field] = Math.max(0, current + (int) params.delta);"
    }
}
