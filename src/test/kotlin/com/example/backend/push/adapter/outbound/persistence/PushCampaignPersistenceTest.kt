package com.example.backend.push.adapter.outbound.persistence

import com.example.backend.push.application.port.outbound.PushCampaign
import com.example.backend.push.application.port.outbound.PushCampaignPersistencePort
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** 어댑터가 메서드마다 직접 커밋하므로 롤백 대신 끝난 뒤 지운다. */
class PushCampaignPersistenceTest
    @Autowired
    constructor(
        private val pushCampaignPersistencePort: PushCampaignPersistencePort,
        private val jdbcTemplate: JdbcTemplate,
    ) : IntegrationTestBase() {
        @AfterEach
        fun cleanUp() {
            jdbcTemplate.update("DELETE FROM push_campaigns WHERE admin_email = ?", ADMIN)
        }

        @Test
        fun `저장 후 대상 수와 큐 발행 수를 갱신한다`() {
            val id =
                checkNotNull(pushCampaignPersistencePort.saveUnlessRecent(campaign(), Instant.now().minusSeconds(600)))

            pushCampaignPersistencePort.updateCounts(id, 3, 2)

            val counts =
                jdbcTemplate.queryForMap("SELECT recipient_count, enqueued_count FROM push_campaigns WHERE id = ?", id)
            assertEquals(3, counts["recipient_count"])
            assertEquals(2, counts["enqueued_count"])
        }

        @Test
        fun `지운 기록은 중복 확인에 걸리지 않는다`() {
            val since = Instant.now().minus(Duration.ofMinutes(10))
            val id = checkNotNull(pushCampaignPersistencePort.saveUnlessRecent(campaign(), since))

            pushCampaignPersistencePort.delete(id)

            assertNotNull(pushCampaignPersistencePort.saveUnlessRecent(campaign(), since))
        }

        @Test
        fun `since 이후 같은 어드민, 제목, 본문 기록이 있으면 저장하지 않고 null 이다`() {
            val since = Instant.now().minus(Duration.ofMinutes(10))
            val id = checkNotNull(pushCampaignPersistencePort.saveUnlessRecent(campaign(), since))

            assertNull(pushCampaignPersistencePort.saveUnlessRecent(campaign(), since))
            assertNotNull(pushCampaignPersistencePort.saveUnlessRecent(campaign().copy(body = "다른 본문"), since))

            jdbcTemplate.update("UPDATE push_campaigns SET created_at = now() - interval '11 minutes' WHERE id = ?", id)
            jdbcTemplate.update("DELETE FROM push_campaigns WHERE admin_email = ? AND body = ?", ADMIN, "다른 본문")
            assertNotNull(pushCampaignPersistencePort.saveUnlessRecent(campaign(), since))
        }

        @Test
        fun `같은 어드민의 같은 문구가 동시에 들어와도 하나만 저장한다`() {
            val since = Instant.now().minus(Duration.ofMinutes(10))
            val start = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(2)
            try {
                val results =
                    (1..2).map {
                        pool.submit<Long?> {
                            start.await()
                            pushCampaignPersistencePort.saveUnlessRecent(campaign(), since)
                        }
                    }
                start.countDown()

                assertEquals(1, results.map { it.get(10, TimeUnit.SECONDS) }.count { it != null })
                assertEquals(
                    1,
                    jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM push_campaigns WHERE admin_email = ?",
                        Int::class.java,
                        ADMIN,
                    ),
                )
            } finally {
                pool.shutdownNow()
            }
        }

        private fun campaign() = PushCampaign(ADMIN, "제목", "본문", "https://example.com", false, 3)

        private companion object {
            const val ADMIN = "persistence-push-admin@example.com"
        }
    }
