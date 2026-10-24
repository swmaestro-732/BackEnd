package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTable
import com.example.backend.course.application.port.inbound.dto.CourseOwnerSummary
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.support.IntegrationTestBase
import com.example.backend.support.SqlCapture
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.time.Instant

class CourseOwnerSummaryPersistenceTest
    @Autowired
    constructor(
        private val query: CoursePersistencePort,
    ) : IntegrationTestBase() {
        @Test
        fun `작성자 요약은 활성 미삭제 코스만 단일 조회로 반환한다`() {
            transaction {
                val active = insertCourse("활성 코스")
                val hidden = insertCourse("숨긴 코스", status = CourseStatus.HIDDEN)
                val suspended = insertCourse("정지 코스", status = CourseStatus.SUSPENDED)
                val deleted = insertCourse("삭제 코스", deletedAt = Instant.parse("2026-10-24T09:00:00Z"))
                val draft = insertCourse("비공개 초안", published = false)

                val (result, statements) =
                    SqlCapture.record {
                        query.findOwnerSummaries(listOf(active, hidden, suspended, deleted, draft, Long.MAX_VALUE))
                    }

                assertThat(result).isEqualTo(
                    mapOf(
                        active to CourseOwnerSummary(active, AUTHOR_ID, "활성 코스"),
                        draft to CourseOwnerSummary(draft, AUTHOR_ID, "비공개 초안"),
                    ),
                )
                assertThat(statements).hasSize(1)
                assertThat(statements.single().sql.lowercase())
                    .contains("select", "courses", "user_id", "title")
                    .doesNotContain(" join ", "course_places", "course_tags", "saved_courses", "course_likes")
                rollback()
            }
        }

        @Test
        fun `빈 코스 목록이면 쿼리 없이 빈 작성자 요약을 반환한다`() {
            val (result, statements) = SqlCapture.record { query.findOwnerSummaries(emptyList()) }

            assertThat(result).isEmpty()
            assertThat(statements).isEmpty()
        }

        private fun insertCourse(
            title: String,
            status: CourseStatus = CourseStatus.ACTIVE,
            deletedAt: Instant? = null,
            published: Boolean = true,
        ): Long =
            CourseTable
                .insertAndGetId {
                    it[userId] = AUTHOR_ID
                    it[CourseTable.title] = title
                    it[CourseTable.status] = status
                    it[CourseTable.deletedAt] = deletedAt
                    it[isPublished] = published
                    it[visibility] = CourseVisibility.PRIVATE
                }.value

        private companion object {
            const val AUTHOR_ID = 570_950L
        }
    }
