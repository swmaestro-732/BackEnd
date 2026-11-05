package com.example.backend.push.adapter.outbound.persistence

import com.example.backend.push.application.port.outbound.PushCampaign
import com.example.backend.push.application.port.outbound.PushCampaignPersistencePort
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import java.time.Instant

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
        fun `저장 후 큐 발행 수를 갱신한다`() {
            val id = pushCampaignPersistencePort.save(campaign())

            pushCampaignPersistencePort.updateEnqueuedCount(id, 2)

            val enqueuedCount =
                jdbcTemplate.queryForObject(
                    "SELECT enqueued_count FROM push_campaigns WHERE id = ?",
                    Int::class.java,
                    id,
                )
            assertEquals(2, enqueuedCount)
        }

        @Test
        fun `최근 발송은 같은 어드민과 제목과 본문이고 since 이후에 만든 기록만 찾는다`() {
            val id = pushCampaignPersistencePort.save(campaign())
            val since = Instant.now().minus(Duration.ofMinutes(10))

            assertTrue(pushCampaignPersistencePort.existsRecent(ADMIN, "제목", "본문", since))
            assertFalse(pushCampaignPersistencePort.existsRecent(ADMIN, "제목", "다른 본문", since))
            assertFalse(pushCampaignPersistencePort.existsRecent("other@example.com", "제목", "본문", since))

            jdbcTemplate.update("UPDATE push_campaigns SET created_at = now() - interval '11 minutes' WHERE id = ?", id)
            assertFalse(pushCampaignPersistencePort.existsRecent(ADMIN, "제목", "본문", since))
        }

        private fun campaign() = PushCampaign(ADMIN, "제목", "본문", "https://example.com", false, 3)

        private companion object {
            const val ADMIN = "persistence-push-admin@example.com"
        }
    }
