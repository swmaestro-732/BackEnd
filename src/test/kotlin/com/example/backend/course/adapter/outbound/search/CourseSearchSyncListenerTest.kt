package com.example.backend.course.adapter.outbound.search

import com.example.backend.course.application.event.CourseAuthorWithdrawnEvent
import com.example.backend.course.application.event.CourseDeletedEvent
import com.example.backend.course.application.event.CourseSavedEvent
import com.example.backend.course.application.port.outbound.CourseSearchIndexPort
import com.example.backend.course.domain.model.Course
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.course.domain.model.CourseVisibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * AFTER_COMMIT 리스너가 각 이벤트를 검색 인덱스 포트의 대응 메서드로 위임하는지 검증한다.
 * 비동기·트랜잭션 없이 메서드를 직접 호출해 위임만 확인한다(색인 실동작은 OpenSearch 통합테스트가 담당).
 */
class CourseSearchSyncListenerTest {
    private val savedCourses = mutableListOf<Course>()
    private val deletedIds = mutableListOf<Long>()
    private val deletedAuthorIds = mutableListOf<Long>()

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
        }

    private val listener = CourseSearchSyncListener(port)

    @Test
    fun `onCourseSaved 는 코스를 포트에 저장한다`() {
        val course = course(1L)

        listener.onCourseSaved(CourseSavedEvent(course))

        assertEquals(listOf(course), savedCourses)
    }

    @Test
    fun `onCourseDeleted 는 코스 id 를 포트에서 삭제한다`() {
        listener.onCourseDeleted(CourseDeletedEvent(42L))

        assertEquals(listOf(42L), deletedIds)
    }

    @Test
    fun `onCourseAuthorWithdrawn 는 작성자 id 로 포트에서 삭제한다`() {
        listener.onCourseAuthorWithdrawn(CourseAuthorWithdrawnEvent(7L))

        assertEquals(listOf(7L), deletedAuthorIds)
    }

    private fun course(id: Long): Course =
        Course.reconstitute(
            id = id,
            userId = 1L,
            status = CourseStatus.ACTIVE,
            title = "코스 $id",
            description = null,
            coverImageUrl = null,
            category = null,
            area = null,
            areaCode = null,
            visitDate = null,
            visibility = CourseVisibility.PUBLIC,
            isPublished = true,
            likesCnt = 0,
            commentsCnt = 0,
            savesCnt = 0,
            tracingsCnt = 0,
            forkedFromId = null,
            createdAt = null,
            updatedAt = null,
            deletedAt = null,
            tags = emptyList(),
            places = emptyList(),
        )
}
