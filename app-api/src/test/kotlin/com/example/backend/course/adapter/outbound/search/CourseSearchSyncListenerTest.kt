package com.example.backend.course.adapter.outbound.search

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.event.CourseAuthorWithdrawnEvent
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.course.application.port.inbound.CourseReindexUseCase
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * AFTER_COMMIT 리스너가 각 이벤트를 검색 인덱스 포트의 대응 메서드로 위임하는지 검증한다.
 * 비동기·트랜잭션 없이 메서드를 직접 호출해 위임만 확인한다(색인 실동작은 OpenSearch 통합테스트가 담당).
 */
class CourseSearchSyncListenerTest {
    private val reindexedIds = mutableListOf<Long>()
    private val deletedIds = mutableListOf<Long>()
    private val deletedAuthorIds = mutableListOf<Long>()

    private val port =
        object : CourseSearchIndexPort {
            override fun save(course: Course) = Unit

            override fun save(courses: List<Course>) = Unit

            override fun delete(courseId: Long) {
                deletedIds += courseId
            }

            override fun deleteByAuthor(authorId: Long) {
                deletedAuthorIds += authorId
            }
        }

    private val courseReindexUseCase =
        object : CourseReindexUseCase {
            override fun reindexAll(): Int = error("리스너는 전체 재색인을 부르지 않는다")

            override fun reindex(courseId: Long) {
                reindexedIds += courseId
            }
        }

    private val listener = CourseSearchSyncListener(port, courseReindexUseCase)

    @Test
    fun `onCourseSaved 는 코스 id 로 다시 읽어 색인하게 넘긴다`() {
        listener.onCourseSaved(
            CourseSavedEvent(
                courseId = 1L,
                authorId = 1L,
                oldVisibility = null,
                newVisibility = CourseVisibility.PUBLIC,
            ),
        )

        assertEquals(listOf(1L), reindexedIds)
    }

    @Test
    fun `저장 이벤트는 저장된 코스에서 작성자와 새 공개범위를 도출하고 발행 코스만 집계한다`() {
        for (visibility in CourseVisibility.entries) {
            val published = CourseSavedEvent.from(course(1L, visibility = visibility), CourseVisibility.FOLLOWER)
            assertEquals(1L, published.courseId)
            assertEquals(1L, published.authorId)
            assertEquals(CourseVisibility.FOLLOWER, published.oldVisibility)
            assertEquals(visibility, published.newVisibility)

            val draft =
                CourseSavedEvent.from(
                    course(2L, visibility, isPublished = false, userId = 7L),
                    CourseVisibility.FOLLOWER,
                )
            assertEquals(2L, draft.courseId)
            assertEquals(7L, draft.authorId)
            assertEquals(CourseVisibility.FOLLOWER, draft.oldVisibility)
            assertNull(draft.newVisibility)
        }
    }

    @Test
    fun `onCourseDeleted 는 코스 id 를 포트에서 삭제한다`() {
        listener.onCourseDeleted(
            CourseDeletedEvent(courseId = 42L, authorId = 1L, oldVisibility = CourseVisibility.PUBLIC),
        )

        assertEquals(listOf(42L), deletedIds)
    }

    @Test
    fun `onCourseAuthorWithdrawn 는 작성자 id 로 포트에서 삭제한다`() {
        listener.onCourseAuthorWithdrawn(CourseAuthorWithdrawnEvent(7L))

        assertEquals(listOf(7L), deletedAuthorIds)
    }

    private fun course(
        id: Long,
        visibility: CourseVisibility = CourseVisibility.PUBLIC,
        isPublished: Boolean = true,
        userId: Long = 1L,
    ): Course =
        Course.reconstitute(
            id = id,
            userId = userId,
            status = CourseStatus.ACTIVE,
            title = "코스 $id",
            description = null,
            coverImageUrl = null,
            categories = emptyList(),
            area = null,
            areaCode = null,
            visitDate = null,
            visibility = visibility,
            isPublished = isPublished,
            likesCnt = 0,
            commentsCnt = 0,
            savesCnt = 0,
            tracingsCnt = 0,
            duplicatedFromId = null,
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
            tags = emptyList(),
            places = emptyList(),
        )
}
