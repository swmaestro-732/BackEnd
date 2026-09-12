package com.example.backend.course.adapter.outbound.search

import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import com.example.backend.course.domain.model.Course
import io.github.oshai.kotlinlogging.KotlinLogging
import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [CourseSearchIndexPort] 를 OpenSearch 색인으로 구현한다.
 *
 * [OpenSearchClient] 가 없으면(=opensearch.endpoint 미주입, 로컬·CI) no-op, 예외는 warn 로그만 남기고
 * 삼킨다(fail-soft) — 검색은 부가 기능이라 코스 쓰기 흐름을 막지 않는다. alias `course` 에 upsert/delete 한다.
 */
@Component
class OpenSearchCourseIndexAdapter(
    private val clientProvider: ObjectProvider<OpenSearchClient>,
) : CourseSearchIndexPort {
    private val log = KotlinLogging.logger {}

    override fun save(course: Course) {
        val id = course.id ?: return
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op
        try {
            client.index { req -> req.index(INDEX_ALIAS).id(id.toString()).document(course.toDocument()) }
        } catch (e: Exception) {
            log.warn { "course 색인 실패(무시): id=$id — ${e.message}" }
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
                    bulk.operations(
                        documents.map { course ->
                            val document = course.toDocument()
                            org.opensearch.client.opensearch.core.bulk.BulkOperation
                                .Builder()
                                .index { op -> op.index(INDEX_ALIAS).id(course.id.toString()).document(document) }
                                .build()
                        },
                    )
                }
            // bulk 는 예외 없이 200 을 주면서 개별 문서만 거부될 수 있다(매핑 충돌 등) → 항목별 실패를 집계해 로그로 드러낸다.
            if (response.errors()) {
                val failed = response.items().count { it.error() != null }
                log.warn { "course 색인 부분 실패(무시): $failed/${documents.size}건 실패" }
            }
        } catch (e: Exception) {
            log.warn { "course 색인 실패(무시): ${courses.size}건 — ${e.message}" }
        }
    }

    override fun delete(courseId: Long) {
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op
        try {
            client.delete { req -> req.index(INDEX_ALIAS).id(courseId.toString()) }
        } catch (e: Exception) {
            log.warn { "course 색인 삭제 실패(무시): id=$courseId — ${e.message}" }
        }
    }

    override fun deleteByAuthor(authorId: Long) {
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op
        try {
            client.deleteByQuery { req ->
                req.index(INDEX_ALIAS).query { q ->
                    q.term { t -> t.field("userId").value { v -> v.stringValue(authorId.toString()) } }
                }
            }
        } catch (e: Exception) {
            log.warn { "course 작성자 색인 삭제 실패(무시): authorId=$authorId — ${e.message}" }
        }
    }

    private fun Course.toDocument(): CourseDocument =
        CourseDocument(
            // 색인 대상은 id 가 채워진(영속화된) 코스뿐이다 — 호출부(save)에서 null 을 걸러낸다.
            id = id!!,
            title = title,
            description = description,
            area = area,
            category = category?.name,
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
        const val INDEX_ALIAS = "course"
    }
}
