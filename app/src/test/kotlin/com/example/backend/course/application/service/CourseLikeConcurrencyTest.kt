package com.example.backend.course.application.service

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseLikeTable
import com.example.backend.course.adapter.outbound.persistence.exposed.CourseTable
import com.example.backend.course.domain.model.CourseStatus
import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.adapter.outbound.persistence.exposed.UserTable
import com.example.backend.user.domain.model.UserStatus
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 좋아요 동시성 정합 테스트(실제 PostgreSQL) — 같은 코스에 서로 다른 사용자 N명이 동시에 좋아요하면 likes_cnt 가 정확히 N 이 된다.
 * 원자적 +1 … RETURNING(UPDATE 가 잡는 행 락 아래에서 DB 가 재계산)으로 로스트 업데이트·이중 집계가 없음을 검증한다.
 *
 * 서비스가 각자 트랜잭션을 커밋하므로 이 테스트는 실제 행을 남긴다 — finally 에서 만든 코스·좋아요·사용자를 정리한다.
 */
class CourseLikeConcurrencyTest
    @Autowired
    constructor(
        private val courseLikeService: CourseLikeService,
    ) : IntegrationTestBase() {
        @Test
        fun `같은 코스에 N명이 동시에 좋아요해도 likes_cnt 는 정확히 N 이다`() {
            val threadCount = 20
            val courseId = insertCourse()
            val userIds = insertUsers(threadCount)

            val executor = Executors.newFixedThreadPool(threadCount)
            val startGate = CountDownLatch(1)
            val doneGate = CountDownLatch(threadCount)
            val successes = AtomicInteger(0)
            try {
                userIds.forEach { userId ->
                    executor.submit {
                        startGate.await()
                        try {
                            courseLikeService.like(userId, courseId)
                            successes.incrementAndGet()
                        } finally {
                            doneGate.countDown()
                        }
                    }
                }
                startGate.countDown() // 동시에 출발
                assertEquals(true, doneGate.await(30, TimeUnit.SECONDS)) // 데드락 없이 완료

                assertEquals(threadCount, successes.get()) // 서로 다른 사용자라 전원 성공
                assertEquals(threadCount, likesCnt(courseId)) // 로스트 업데이트가 있으면 N 미만이 된다
                assertEquals(threadCount.toLong(), aliveLikeCount(courseId)) // 좋아요 행도 정확히 N
            } finally {
                executor.shutdownNow()
                cleanUp(courseId, userIds)
            }
        }

        private fun insertCourse(): Long =
            transaction {
                CourseTable
                    .insert {
                        it[status] = CourseStatus.ACTIVE
                        it[userId] = 1L
                        it[title] = "동시성좋아요코스"
                        it[isPublished] = true
                        it[visibility] = CourseVisibility.PUBLIC
                        it[likesCnt] = 0
                    }[CourseTable.id]
                    .value
            }

        private fun insertUsers(count: Int): List<Long> {
            // nickname(varchar20)·handle(varchar30) 는 UNIQUE — 실행마다 짧은 고유 접두로 충돌을 피한다.
            val run = System.nanoTime() % 1_000_000
            return transaction {
                (1..count).map { i ->
                    UserTable
                        .insert {
                            it[nickname] = "cl${run}_$i"
                            it[handle] = "clh${run}_$i"
                            it[status] = UserStatus.ACTIVE
                            it[followersCnt] = 0
                            it[followingsCnt] = 0
                            it[publicCoursesCnt] = 0
                            it[followerCoursesCnt] = 0
                            it[privateCoursesCnt] = 0
                        }[UserTable.id]
                        .value
                }
            }
        }

        private fun likesCnt(courseId: Long): Int =
            transaction {
                CourseTable.selectAll().where { CourseTable.id eq courseId }.single()[CourseTable.likesCnt]
            }

        private fun aliveLikeCount(courseId: Long): Long =
            transaction {
                CourseLikeTable
                    .selectAll()
                    .where { CourseLikeTable.courseId eq courseId }
                    .count()
            }

        private fun cleanUp(
            courseId: Long,
            userIds: List<Long>,
        ) {
            transaction {
                CourseLikeTable.deleteWhere { CourseLikeTable.courseId eq courseId }
                CourseTable.deleteWhere { CourseTable.id eq courseId }
                userIds.forEach { id -> UserTable.deleteWhere { UserTable.id eq id } }
            }
        }
    }
