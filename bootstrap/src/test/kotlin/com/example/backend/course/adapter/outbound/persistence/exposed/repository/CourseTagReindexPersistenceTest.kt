package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseEntity
import com.example.backend.course.adapter.outbound.persistence.exposed.TagTable
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * [CourseTagRepository.findNamesByCourseIds] 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * 재색인 경로가 태그를 빈 채 색인하던 버그(태그 필터 검색 누락) 수정의 핵심 — 코스별 태그명을 배치로 읽는지 검증한다.
 * course_tags 는 courses·tags 를 FK 로 참조하므로 실제 코스·태그 행을 만든다. transaction { ... rollback() } 로 격리.
 */
class CourseTagReindexPersistenceTest
    @Autowired
    constructor(
        private val courseTagRepository: CourseTagRepository,
    ) : IntegrationTestBase() {
        @Test
        fun `findNamesByCourseIds 는 여러 코스의 태그명을 코스별로 묶어 읽는다`() {
            transaction {
                val c1 = insertCourse()
                val c2 = insertCourse()
                val worth = TagTable.insertAndGetId { it[name] = "가성비" }.value
                val view = TagTable.insertAndGetId { it[name] = "뷰맛집" }.value

                courseTagRepository.linkAll(c1, listOf(worth, view))
                courseTagRepository.linkAll(c2, listOf(worth))

                val byCourse = courseTagRepository.findNamesByCourseIds(listOf(c1, c2))
                assertThat(byCourse[c1]).containsExactlyInAnyOrder("가성비", "뷰맛집")
                assertThat(byCourse[c2]).containsExactly("가성비")
                rollback()
            }
        }

        @Test
        fun `태그가 없는 코스는 맵에 없고, 빈 입력은 빈 맵이다`() {
            transaction {
                val c1 = insertCourse()
                assertThat(courseTagRepository.findNamesByCourseIds(listOf(c1))).doesNotContainKey(c1)
                assertThat(courseTagRepository.findNamesByCourseIds(emptyList())).isEmpty()
                rollback()
            }
        }

        /** course_tags 의 FK(course_id→courses) 를 만족시킬 실제 course 하나. */
        private fun insertCourse(): Long =
            CourseEntity
                .new {
                    status = CourseStatus.ACTIVE
                    userId = 1L
                    title = "태그대상 코스"
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
