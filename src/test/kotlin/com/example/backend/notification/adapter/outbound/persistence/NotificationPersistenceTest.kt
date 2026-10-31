package com.example.backend.notification.adapter.outbound.persistence

import com.example.backend.notification.application.port.inbound.dto.UpdateNotificationSettingsCommand
import com.example.backend.notification.application.port.outbound.NotificationPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationSettingsPersistencePort
import com.example.backend.notification.domain.model.NotificationBundle
import com.example.backend.notification.domain.model.NotificationPayload
import com.example.backend.notification.domain.model.NotificationSettings
import com.example.backend.notification.domain.model.NotificationTargetType
import com.example.backend.notification.domain.model.NotificationType
import com.example.backend.support.IntegrationTestBase
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class NotificationPersistenceTest
    @Autowired
    constructor(
        private val port: NotificationPersistencePort,
        private val settingsPort: NotificationSettingsPersistencePort,
    ) : IntegrationTestBase() {
        @Test
        fun `같은 묶음을 업서트하면 한 행의 인원과 마지막 행동자 스냅샷을 갱신한다`() {
            transaction {
                val first = bundle()
                assertThat(port.upsert(first)).isTrue()
                val inserted = port.findPage(USER_ID, null, 10).single()
                // 같은 트랜잭션 안에서는 now() 가 같으므로 기존 갱신 시각을 과거로 돌려 재업서트의 갱신을 구분한다.
                exec("UPDATE notifications SET updated_at = updated_at - interval '1 hour' WHERE id = ${inserted.id}")
                val pastUpdatedAt = port.findPage(USER_ID, null, 10).single().updatedAt
                val latest =
                    first.copy(
                        lastActorId = 570_902L,
                        payload = NotificationPayload("새로운 '작성자' \"인용\"", "https://example.com/new.jpg", "바뀐 코스"),
                    )

                assertThat(port.upsert(latest)).isFalse()

                val rows = port.findPage(USER_ID, null, 10)
                assertThat(rows.map { it.id }).containsExactly(inserted.id)
                val updated = rows.single()
                assertThat(updated.type).isEqualTo(NotificationType.COURSE_LIKED)
                assertThat(updated.targetType).isEqualTo(NotificationTargetType.COURSE)
                assertThat(updated.targetId).isEqualTo(first.targetId)
                assertThat(updated.actorCount).isEqualTo(2)
                assertThat(updated.lastActorId).isEqualTo(latest.lastActorId)
                assertThat(updated.actorNickname).isEqualTo(latest.payload.actorNickname)
                assertThat(updated.actorProfileImageUrl).isEqualTo(latest.payload.actorProfileImageUrl)
                assertThat(updated.courseTitle).isEqualTo(latest.payload.courseTitle)
                assertThat(updated.isRead).isFalse()
                assertThat(updated.createdAt).isEqualTo(inserted.createdAt)
                assertThat(updated.updatedAt).isAfter(pastUpdatedAt)
                rollback()
            }
        }

        @Test
        fun `다른 창이면 새 행을 만들고 커서 이전의 본인 알림만 내림차순으로 조회한다`() {
            transaction {
                val first = bundle()
                port.upsert(first)
                val firstId = port.findPage(USER_ID, null, 10).single().id
                assertThat(port.upsert(first.copy(windowStart = first.windowStart.plusSeconds(600)))).isTrue()
                val secondId = port.findPage(USER_ID, null, 1).single().id
                port.upsert(first.copy(userId = OTHER_USER_ID))

                assertThat(port.findPage(USER_ID, null, 10).map { it.id }).containsExactly(secondId, firstId)
                assertThat(port.findPage(USER_ID, secondId, 10).map { it.id }).containsExactly(firstId)
                assertThat(port.findPage(USER_ID, firstId, 10)).isEmpty()
                assertThat(port.countUnread(USER_ID)).isEqualTo(2L)
                assertThat(port.countUnread(OTHER_USER_ID)).isEqualTo(1L)
                rollback()
            }
        }

        @Test
        fun `읽음 처리는 본인 미읽음만 갱신하고 반복과 없는 알림은 0을 반환한다`() {
            transaction {
                port.upsert(bundle())
                val id = port.findPage(USER_ID, null, 10).single().id

                assertThat(port.markRead(OTHER_USER_ID, id)).isZero()
                assertThat(port.markRead(USER_ID, Long.MAX_VALUE)).isZero()
                assertThat(port.countUnread(USER_ID)).isEqualTo(1L)
                assertThat(port.markRead(USER_ID, id)).isEqualTo(1)
                assertThat(port.markRead(USER_ID, id)).isZero()
                assertThat(port.countUnread(USER_ID)).isZero()
                assertThat(port.findPage(USER_ID, null, 10).single().isRead).isTrue()
                rollback()
            }
        }

        @Test
        fun `읽은 묶음에 같은 행동자가 다시 기록되면 인원이 늘고 미읽음으로 돌아간다`() {
            transaction {
                val activity = bundle()
                port.upsert(activity)
                val id = port.findPage(USER_ID, null, 10).single().id
                port.markRead(USER_ID, id)

                assertThat(port.upsert(activity)).isFalse()

                val rows = port.findPage(USER_ID, null, 10)
                assertThat(rows.map { it.id }).containsExactly(id)
                assertThat(rows.single().actorCount).isEqualTo(2)
                assertThat(rows.single().isRead).isFalse()
                assertThat(port.countUnread(USER_ID)).isEqualTo(1L)
                rollback()
            }
        }

        @Test
        fun `모두 읽음은 본인의 미읽음 수만 반환하고 다른 사용자는 유지한다`() {
            transaction {
                port.upsert(bundle())
                val readId = port.findPage(USER_ID, null, 10).single().id
                port.markRead(USER_ID, readId)
                port.upsert(bundle().copy(targetId = 2L))
                port.upsert(bundle().copy(targetId = 3L))
                port.upsert(bundle().copy(userId = OTHER_USER_ID))

                assertThat(port.markAllRead(USER_ID)).isEqualTo(2)
                assertThat(port.markAllRead(USER_ID)).isZero()
                assertThat(port.countUnread(USER_ID)).isZero()
                assertThat(port.countUnread(OTHER_USER_ID)).isEqualTo(1L)
                assertThat(port.findPage(USER_ID, null, 10).map { it.isRead }).containsExactly(true, true, true)
                rollback()
            }
        }

        @Test
        fun `설정 행이 없으면 null을 반환하고 부분 업서트는 나머지 기본값을 유지한다`() {
            transaction {
                assertThat(settingsPort.findByUser(USER_ID)).isNull()

                val saved = settingsPort.update(USER_ID, UpdateNotificationSettingsCommand(activityLike = false))

                assertThat(saved).isEqualTo(NotificationSettings.DEFAULT.copy(activityLike = false))
                assertThat(settingsPort.findByUser(USER_ID)).isEqualTo(saved)
                assertThat(settingsPort.findByUser(OTHER_USER_ID)).isNull()
                rollback()
            }
        }

        @Test
        fun `설정 부분 수정의 null은 기존값을 유지하고 모든 카테고리를 따로 수정한다`() {
            transaction {
                settingsPort.update(
                    USER_ID,
                    UpdateNotificationSettingsCommand(activityLike = false, activityFollow = false),
                )

                val saved =
                    settingsPort.update(
                        USER_ID,
                        UpdateNotificationSettingsCommand(
                            activityComment = false,
                            activityCourse = false,
                            activityReview = false,
                        ),
                    )

                assertThat(saved).isEqualTo(NotificationSettings(false, false, false, false, false))
                assertThat(settingsPort.update(USER_ID, UpdateNotificationSettingsCommand())).isEqualTo(saved)
                assertThat(settingsPort.update(USER_ID, UpdateNotificationSettingsCommand(activityLike = true)))
                    .isEqualTo(saved.copy(activityLike = true))
                rollback()
            }
        }

        private fun bundle() =
            NotificationBundle(
                userId = USER_ID,
                type = NotificationType.COURSE_LIKED,
                targetType = NotificationTargetType.COURSE,
                targetId = 570_901L,
                windowStart = Instant.parse("2026-10-24T09:50:00Z"),
                lastActorId = 570_900L,
                payload = NotificationPayload("첫 행동자", null, "알림 코스"),
            )

        private companion object {
            const val USER_ID = 570_901L
            const val OTHER_USER_ID = 570_902L
        }
    }
