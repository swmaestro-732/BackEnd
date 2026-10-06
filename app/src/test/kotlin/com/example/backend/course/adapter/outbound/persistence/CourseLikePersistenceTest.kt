package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.CourseLikeTable
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseLikeRepository
import com.example.backend.course.application.port.outbound.CourseLikePersistencePort
import com.example.backend.support.IntegrationTestBase
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

/**
 * course_likes 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * 삽입·존재검사·하드 삭제·유니크(user_id, course_id)·취소 후 재좋아요·배치 조회를 검증한다.
 * course_likes 는 FK 가 없어(cross-domain) 사용자/코스 시드 없이 임의 id 로 격리 검증한다. transaction{ rollback() } 으로 격리.
 */
class CourseLikePersistenceTest
    @Autowired
    constructor(
        private val port: CourseLikePersistencePort,
        private val courseLikeRepository: CourseLikeRepository,
    ) : IntegrationTestBase() {
        @Test
        fun `insert 는 좋아요 행을 만들고 existsLike 가 true 다`() {
            transaction {
                port.insert(userId = 1L, courseId = 100L)

                assertTrue(port.existsLike(1L, 100L))
                assertFalse(port.existsLike(1L, 101L)) // 좋아요하지 않은 코스
                assertFalse(port.existsLike(2L, 100L)) // 다른 사용자
                rollback()
            }
        }

        @Test
        fun `중복 좋아요는 유니크 인덱스가 막는다`() {
            transaction {
                port.insert(userId = 1L, courseId = 102L)

                // uq_course_likes_user_course 위반 → 23505. 서비스 사전검사와 별개인 최종 방어선.
                val ex = assertThrows<ExposedSQLException> { port.insert(userId = 1L, courseId = 102L) }
                assertEquals("23505", ex.sqlState)
                rollback()
            }
        }

        @Test
        fun `deleteByUserAndCourse 는 행을 하드 삭제하고 두 번째 호출은 false 다 (멱등)`() {
            transaction {
                port.insert(userId = 1L, courseId = 103L)

                assertTrue(port.deleteByUserAndCourse(1L, 103L))

                assertNull(row(1L, 103L)) // 행이 남지 않는다(하드 삭제)
                assertFalse(port.existsLike(1L, 103L))

                assertFalse(port.deleteByUserAndCourse(1L, 103L)) // 이미 없음 = 0행
                rollback()
            }
        }

        @Test
        fun `취소 후 같은 코스를 다시 좋아요하면 살아있는 행 하나만 남는다`() {
            transaction {
                port.insert(userId = 1L, courseId = 104L)
                port.deleteByUserAndCourse(1L, 104L)

                port.insert(userId = 1L, courseId = 104L)

                assertTrue(port.existsLike(1L, 104L))
                val count =
                    CourseLikeTable
                        .selectAll()
                        .where { (CourseLikeTable.userId eq 1L) and (CourseLikeTable.courseId eq 104L) }
                        .count()
                assertEquals(1L, count)
                rollback()
            }
        }

        @Test
        fun `findLikedCourseIds 는 좋아요한 코스 id 를 배치 반환한다`() {
            transaction {
                port.insert(userId = 1L, courseId = 200L)
                port.insert(userId = 1L, courseId = 201L)
                port.insert(userId = 1L, courseId = 202L)
                port.deleteByUserAndCourse(1L, 202L) // 취소한 것은 제외돼야 한다

                val liked = courseLikeRepository.findLikedCourseIds(1L, listOf(200L, 201L, 202L, 203L))

                assertEquals(setOf(200L, 201L), liked)
                assertTrue(courseLikeRepository.findLikedCourseIds(1L, emptyList()).isEmpty())
                rollback()
            }
        }

        @Test
        fun `deleteAllByUser 는 사용자 좋아요를 전부 지우고 삭제된 course_id 를 반환한다`() {
            transaction {
                // 다른 테스트 시드(컨트롤러 픽스처가 커밋해 둔 행 등)와 겹치지 않는 전용 userId 로 격리한다.
                val u1 = 970_001L
                val u2 = 970_002L
                port.insert(userId = u1, courseId = 300L)
                port.insert(userId = u1, courseId = 301L)
                port.insert(userId = u2, courseId = 300L)

                assertEquals(setOf(300L, 301L), port.deleteAllByUser(u1).toSet()) // 삭제된 것만 반환

                assertFalse(port.existsLike(u1, 300L))
                assertTrue(port.deleteAllByUser(u1).isEmpty()) // 이미 없으면 빈 목록(멱등)
                assertTrue(port.existsLike(u2, 300L)) // 다른 사용자 좋아요는 남는다
                rollback()
            }
        }

        private fun row(
            userId: Long,
            courseId: Long,
        ) = CourseLikeTable
            .selectAll()
            .where { (CourseLikeTable.userId eq userId) and (CourseLikeTable.courseId eq courseId) }
            .singleOrNull()
    }
