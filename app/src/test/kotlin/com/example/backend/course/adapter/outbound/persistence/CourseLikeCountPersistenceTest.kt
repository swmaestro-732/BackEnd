package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTable
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseRepository
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.support.IntegrationTestBase
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.time.Clock

/**
 * courses.likes_cnt 원자 증감(UPDATE … RETURNING) 통합 테스트(실제 PostgreSQL). [CourseRepository] 대상.
 * transaction{ rollback() } 으로 격리한다.
 */
class CourseLikeCountPersistenceTest
    @Autowired
    constructor(
        private val courseRepository: CourseRepository,
    ) : IntegrationTestBase() {
        @Test
        fun `incrementLikesCntReturning 은 살아있는 코스의 likes_cnt 를 1 올리고 증가된 값을 반환한다`() {
            transaction {
                val courseId = insertCourse(likesCnt = 2)

                assertEquals(3, courseRepository.incrementLikesCntReturning(courseId))

                assertEquals(3, likesCnt(courseId))
                rollback()
            }
        }

        @Test
        fun `decrementLikesCntReturning 은 살아있는 코스의 likes_cnt 를 1 내리고 감소된 값을 반환한다`() {
            transaction {
                val courseId = insertCourse(likesCnt = 2)

                assertEquals(1, courseRepository.decrementLikesCntReturning(courseId))

                assertEquals(1, likesCnt(courseId))
                rollback()
            }
        }

        @Test
        fun `소프트 삭제된 코스는 증감이 null 이고 카운터도 바뀌지 않는다`() {
            transaction {
                val courseId = insertCourse(likesCnt = 5, deleted = true)

                assertNull(courseRepository.incrementLikesCntReturning(courseId))
                assertNull(courseRepository.decrementLikesCntReturning(courseId))
                assertEquals(5, likesCnt(courseId))
                rollback()
            }
        }

        @Test
        fun `readLikesCnt 는 살아있는 코스의 현재 likes_cnt 를 반환한다`() {
            transaction {
                val courseId = insertCourse(likesCnt = 7)

                assertEquals(7, courseRepository.readLikesCnt(courseId))
                rollback()
            }
        }

        @Test
        fun `readLikesCnt 는 없는 코스나 소프트 삭제된 코스면 null 이다`() {
            transaction {
                val deleted = insertCourse(likesCnt = 1, deleted = true)

                assertNull(courseRepository.readLikesCnt(deleted))
                assertNull(courseRepository.readLikesCnt(999_999L))
                rollback()
            }
        }

        private fun insertCourse(
            likesCnt: Int,
            deleted: Boolean = false,
        ): Long =
            CourseTable
                .insert {
                    it[status] = if (deleted) CourseStatus.DELETED else CourseStatus.ACTIVE
                    it[userId] = 1L
                    it[title] = "좋아요코스"
                    it[isPublished] = true
                    it[visibility] = CourseVisibility.PUBLIC
                    it[CourseTable.likesCnt] = likesCnt
                    if (deleted) it[deletedAt] = Clock.System.now()
                }[CourseTable.id]
                .value

        private fun likesCnt(courseId: Long): Int =
            CourseTable
                .selectAll()
                .where { CourseTable.id eq courseId }
                .single()[CourseTable.likesCnt]
    }
