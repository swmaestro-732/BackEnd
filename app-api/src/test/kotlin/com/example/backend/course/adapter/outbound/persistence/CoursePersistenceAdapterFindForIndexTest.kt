package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseEntity
import com.example.backend.course.adapter.outbound.persistence.exposed.TagTable
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseTagRepository
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * [CoursePersistenceAdapter.findForIndex] 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * 재색인 경로에서 태그가 채워지는지 검증한다 — 태그 없이 색인하면 태그 필터 검색에서 누락된다.
 * 각 테스트는 transaction { ... rollback() } 으로 격리한다.
 */
class CoursePersistenceAdapterFindForIndexTest
    @Autowired
    constructor(
        private val adapter: CoursePersistenceAdapter,
        private val courseTagRepository: CourseTagRepository,
    ) : IntegrationTestBase() {
        @Test
        fun `findForIndex 는 태그가 연결된 코스를 태그 포함해 반환한다`() {
            transaction {
                val courseId = insertCourse()
                val tagId1 = TagTable.insertAndGetId { it[name] = "감성카페_idx" }.value
                val tagId2 = TagTable.insertAndGetId { it[name] = "데이트_idx" }.value
                courseTagRepository.linkAll(courseId, listOf(tagId1, tagId2))

                val courses = adapter.findForIndex(null, 200)
                val target = courses.find { it.id == courseId }

                assertThat(target).isNotNull()
                assertThat(target!!.tags).containsExactlyInAnyOrder("감성카페_idx", "데이트_idx")

                rollback()
            }
        }

        @Test
        fun `findForIndex 는 태그 없는 코스도 반환하되 tags 가 비어 있다`() {
            transaction {
                val courseId = insertCourse()

                val courses = adapter.findForIndex(null, 200)
                val target = courses.find { it.id == courseId }

                assertThat(target).isNotNull()
                assertThat(target!!.tags).isEmpty()

                rollback()
            }
        }

        @Test
        fun `afterId 를 지정하면 그보다 큰 id 의 코스만 반환한다`() {
            transaction {
                val id1 = insertCourse()
                val id2 = insertCourse()

                val courses = adapter.findForIndex(id1, 200)
                val ids = courses.map { it.id }

                assertThat(ids).contains(id2)
                assertThat(ids).doesNotContain(id1)

                rollback()
            }
        }

        /** course_tags 의 FK(course_id→courses) 를 만족시킬 실제 course 하나. */
        private fun insertCourse(): Long =
            CourseEntity
                .new {
                    status = CourseStatus.ACTIVE
                    userId = 1L
                    title = "색인 대상 코스"
                    coverImageUrl = null
                    description = null
                    category = null
                    area = null
                    areaCode = null
                    visitDate = null
                    isPublished = true
                    visibility = CourseVisibility.PUBLIC
                }.id.value
    }
