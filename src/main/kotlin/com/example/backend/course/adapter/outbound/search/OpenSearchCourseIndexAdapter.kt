package com.example.backend.course.adapter.outbound.search

import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import com.example.backend.course.domain.model.Course
import org.opensearch.client.opensearch.OpenSearchClient
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

    override fun index(course: Course) {
        val id = course.id ?: return
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op
        try {
            client.index { req -> req.index(INDEX_ALIAS).id(id.toString()).document(course.toDocument()) }
        } catch (e: Exception) {
            log.warn("course 색인 실패(무시): id={} — {}", id, e.message)
        }
    }

    override fun delete(courseId: Long) {
        val client = clientProvider.ifAvailable ?: return // endpoint 미설정 → no-op
        try {
            client.delete { req -> req.index(INDEX_ALIAS).id(courseId.toString()) }
        } catch (e: Exception) {
            log.warn("course 색인 삭제 실패(무시): id={} — {}", courseId, e.message)
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
            log.warn("course 작성자 색인 삭제 실패(무시): authorId={} — {}", authorId, e.message)
        }
    }

    private fun Course.toDocument(): CourseDocument =
        CourseDocument(
            title = title,
            description = description,
            area = area,
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

/** OpenSearch 색인 문서 — course 인덱스 매핑(opensearch/course.json)과 필드가 일치한다. createdAt 은 epoch millis(date 매핑 호환). */
data class CourseDocument(
    val title: String,
    val description: String?,
    val area: String?,
    val visibility: String,
    val isPublished: Boolean,
    val userId: String,
    val likesCnt: Int,
    val savesCnt: Int,
    val createdAt: Long?,
)
