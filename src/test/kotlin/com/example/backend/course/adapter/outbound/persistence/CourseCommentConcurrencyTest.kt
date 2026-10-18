package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 댓글 수 카운터(courses.comments_cnt)의 동시성 검증.
 *
 * 같은 코스에 [THREADS]건의 증가를 동시에 날려, 실제 구현(원자적 SQL 증감)이 손실 없이 정확히 N만큼
 * 올라가는지 확인하고([원자적 증감은 동시 요청 N건에서 정확히 N만큼 증가한다]),
 * 반대로 순진한 read-modify-write가 왜 위험한지를 같은 부하에서 수치로 드러낸다
 * ([순진한 read-modify-write 는 동시 요청에서 lost update 가 발생한다]).
 */
@Sql(scripts = ["/sql/course-comment-concurrency-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseCommentConcurrencyTest
    @Autowired
    constructor(
        private val coursePersistencePort: CoursePersistencePort,
        private val jdbcTemplate: JdbcTemplate,
        transactionManager: PlatformTransactionManager,
    ) : IntegrationTestBase() {
        private val tx = TransactionTemplate(transactionManager)

        @Test
        fun `원자적 증감은 동시 요청 N건에서 정확히 N만큼 증가한다`() {
            // 실제 구현 경로: increaseCommentsCount 를 스레드마다 독립 트랜잭션으로 동시에 호출한다.
            val failures =
                runConcurrently { barrier ->
                    barrier.await(5, TimeUnit.SECONDS) // 모두 동시에 출발
                    tx.execute { coursePersistencePort.increaseCommentsCount(COURSE_ID) }
                }

            // 원자 SQL(comments_cnt = comments_cnt + 1)은 행 잠금으로 직렬화돼 손실이 없다.
            assertEquals(0, failures, "모든 증감 트랜잭션이 성공해야 한다")
            assertEquals(THREADS, commentsCnt(), "원자적 증감은 lost update 가 없어 정확히 N이어야 한다")
        }

        @Test
        fun `순진한 read-modify-write 는 동시 요청에서 lost update 가 발생한다`() {
            // 안티패턴 재현: 값을 앱으로 읽고 1 더해 되쓴다. 모든 스레드가 읽기를 마친 뒤 쓰도록 barrier 로 경합을 확정한다.
            val failures =
                runConcurrently { barrier ->
                    val current =
                        jdbcTemplate.queryForObject(
                            "SELECT comments_cnt FROM courses WHERE id = ?",
                            Int::class.java,
                            COURSE_ID,
                        )!!
                    barrier.await(5, TimeUnit.SECONDS) // 모든 스레드가 같은 값(0)을 읽은 뒤에야 쓰기로 넘어간다
                    jdbcTemplate.update("UPDATE courses SET comments_cnt = ? WHERE id = ?", current + 1, COURSE_ID)
                }

            // 모두 0을 읽고 1을 써서 최종값이 N에 한참 못 미친다 — 손실을 수치로 드러낸다.
            assertEquals(0, failures, "쓰기 자체는 모두 성공한다(손실은 값에서 드러난다)")
            assertTrue(commentsCnt() < THREADS, "순진한 방식은 lost update 로 N($THREADS)보다 작아야 한다: 실제=${commentsCnt()}")
        }

        private fun commentsCnt(): Int =
            jdbcTemplate.queryForObject("SELECT comments_cnt FROM courses WHERE id = ?", Int::class.java, COURSE_ID)!!

        /**
         * [THREADS]개 스레드에서 [task]를 동시에 실행하고 실패한 스레드 수를 돌려준다.
         * [task]에 넘기는 barrier 로 스레드들이 원하는 지점에서 서로를 기다리게 해 경합 시점을 맞춘다.
         */
        private fun runConcurrently(task: (CyclicBarrier) -> Unit): Int {
            val pool = Executors.newFixedThreadPool(THREADS)
            val barrier = CyclicBarrier(THREADS)
            val failures = AtomicInteger(0)
            try {
                val futures =
                    (1..THREADS).map {
                        pool.submit {
                            try {
                                task(barrier)
                            } catch (e: Exception) {
                                failures.incrementAndGet()
                            }
                        }
                    }
                futures.forEach { it.get(30, TimeUnit.SECONDS) }
            } finally {
                pool.shutdownNow()
            }
            return failures.get()
        }

        private companion object {
            const val THREADS = 50
            const val COURSE_ID = 1L
        }
    }
