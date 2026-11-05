package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.common.geo.Coordinate
import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.application.port.outbound.UserLastLocationPersistencePort
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 위치 저장과 탈퇴가 겹칠 때 탈퇴한 사용자의 위치 행이 남지 않는지 실제 커밋으로 검증한다.
 * 탈퇴(UserService.withdraw)는 users 행을 FOR UPDATE 로 잡은 채 정리하고 deleted_at 을 찍는다.
 */
class UserLastLocationWithdrawRaceTest
    @Autowired
    constructor(
        private val userLastLocationPersistencePort: UserLastLocationPersistencePort,
        private val jdbcTemplate: JdbcTemplate,
        transactionManager: PlatformTransactionManager,
    ) : IntegrationTestBase() {
        private val tx = TransactionTemplate(transactionManager)

        @BeforeEach
        fun setUp() {
            cleanUp()
            jdbcTemplate.update("INSERT INTO users (id, nickname) VALUES (?, '탈퇴경합')", USER_ID)
        }

        @AfterEach
        fun cleanUp() {
            jdbcTemplate.update("DELETE FROM user_last_locations WHERE user_id = ?", USER_ID)
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", USER_ID)
        }

        @Test
        fun `탈퇴가 users 행을 잡고 있으면 커밋까지 기다렸다가 저장하지 않는다`() {
            val locked = CountDownLatch(1)
            val pool = Executors.newSingleThreadExecutor()
            try {
                val withdraw =
                    pool.submit {
                        tx.execute {
                            jdbcTemplate.queryForList("SELECT id FROM users WHERE id = ? FOR UPDATE", USER_ID)
                            locked.countDown()
                            Thread.sleep(HOLD_MILLIS)
                            jdbcTemplate.update("UPDATE users SET deleted_at = now() WHERE id = ?", USER_ID)
                        }
                    }
                assertTrue(locked.await(5, TimeUnit.SECONDS))

                val startedAt = System.nanoTime()
                tx.execute { userLastLocationPersistencePort.saveIfActive(USER_ID, Coordinate(37.544, 127.056)) }
                val waitedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
                withdraw.get(10, TimeUnit.SECONDS)

                assertTrue(waitedMillis >= HOLD_MILLIS / 2, "탈퇴 커밋을 기다려야 한다: ${waitedMillis}ms")
                assertEquals(0, locationCount(), "탈퇴한 사용자의 위치 행이 남으면 안 된다")
            } finally {
                pool.shutdownNow()
            }
        }

        private fun locationCount(): Int =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM user_last_locations WHERE user_id = ?",
                Int::class.java,
                USER_ID,
            )!!

        private companion object {
            const val USER_ID = 780301L
            const val HOLD_MILLIS = 800L
        }
    }
