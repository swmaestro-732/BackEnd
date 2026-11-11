package com.example.backend.course.adapter.outbound.search

import com.example.backend.bootstrap.config.AsyncConfig
import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.event.CourseAuthorWithdrawnEvent
import com.example.backend.course.application.event.CourseCounter
import com.example.backend.course.application.event.CourseCounterChangedEvent
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.scheduling.annotation.Async

/**
 * AFTER_COMMIT 리스너가 각 이벤트를 검색 인덱스 포트의 대응 메서드로 위임하는지 검증한다.
 * 비동기·트랜잭션 없이 메서드를 직접 호출해 위임만 확인한다(색인 실동작은 OpenSearch 통합테스트가 담당).
 */
class CourseSearchSyncListenerTest {
    private val savedCourses = mutableListOf<Course>()
    private val deletedIds = mutableListOf<Long>()
    private val deletedAuthorIds = mutableListOf<Long>()
    private val adjusted = mutableListOf<Triple<List<Long>, CourseCounter, Int>>()

    private val port =
        object : CourseSearchIndexPort {
            override fun save(course: Course) {
                savedCourses += course
            }

            override fun save(courses: List<Course>) = Unit

            override fun delete(courseId: Long) {
                deletedIds += courseId
            }

            override fun deleteByAuthor(authorId: Long) {
                deletedAuthorIds += authorId
            }

            override fun adjustCounter(
                courseIds: List<Long>,
                counter: CourseCounter,
                delta: Int,
            ) {
                adjusted += Triple(courseIds, counter, delta)
            }
        }

    private val listener = CourseSearchSyncListener(port)

    @Test
    fun `onCourseSaved 는 코스를 포트에 저장한다`() {
        val course = course(1L)

        listener.onCourseSaved(
            CourseSavedEvent(
                newCourse = course,
                oldVisibility = null,
            ),
        )

        assertEquals(listOf(course), savedCourses)
    }

    @Test
    fun `저장 이벤트는 발행 코스만 집계하고 코스 교체 시 파생값도 바뀐다`() {
        for (visibility in CourseVisibility.entries) {
            val published = CourseSavedEvent(course(1L, visibility = visibility), CourseVisibility.FOLLOWER)
            assertEquals(1L, published.authorId)
            assertEquals(CourseVisibility.FOLLOWER, published.oldVisibility)
            assertEquals(visibility, published.newVisibility)

            val draft = published.copy(newCourse = course(2L, visibility, isPublished = false, userId = 7L))
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
    fun `onCourseCounterChanged 는 카운터 종류와 델타를 포트에 넘긴다`() {
        listener.onCourseCounterChanged(CourseCounterChangedEvent(listOf(42L, 43L), CourseCounter.LIKES, delta = -1))

        assertEquals(listOf(Triple(listOf(42L, 43L), CourseCounter.LIKES, -1)), adjusted)
    }

    @Test
    fun `카운터 갱신만 전용 실행기에서 비동기로 돈다`() {
        val counterHandler =
            CourseSearchSyncListener::class.java.getMethod(
                "onCourseCounterChanged",
                CourseCounterChangedEvent::class.java,
            )
        val savedHandler = CourseSearchSyncListener::class.java.getMethod("onCourseSaved", CourseSavedEvent::class.java)

        assertEquals(AsyncConfig.SEARCH_INDEX_COUNTER_EXECUTOR, counterHandler.getAnnotation(Async::class.java)?.value)
        assertNull(savedHandler.getAnnotation(Async::class.java))
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
