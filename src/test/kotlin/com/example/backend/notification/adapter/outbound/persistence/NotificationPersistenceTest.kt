package com.example.backend.notification.adapter.outbound.persistence

import com.example.backend.notification.application.port.inbound.dto.UpdateNotificationSettingsCommand
import com.example.backend.notification.application.port.outbound.NotificationPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationSettingsPersistencePort
import com.example.backend.notification.domain.model.NotificationBundle
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
        private val notificationPersistencePort: NotificationPersistencePort,
        private val notificationSettingsPersistencePort: NotificationSettingsPersistencePort,
    ) : IntegrationTestBase() {
        @Test
        fun `같은 묶음에 다른 행동자를 업서트하면 한 행의 인원과 마지막 행동자 스냅샷을 갱신한다`() {
            transaction {
                val first = bundle()
                notificationPersistencePort.upsert(first)
                val inserted = notificationPersistencePort.findPage(USER_ID, null, 10).single()
                // 같은 트랜잭션 안에서는 now() 가 같으므로 기존 갱신 시각을 과거로 돌려 재업서트의 갱신을 구분한다.
                exec("UPDATE notifications SET updated_at = updated_at - interval '1 hour' WHERE id = ${inserted.id}")
                val pastUpdatedAt = notificationPersistencePort.findPage(USER_ID, null, 10).single().updatedAt
                val latest =
                    first.copy(
                        lastActorId = 570_902L,
                        actorNickname = "새로운 '작성자'",
                        courseTitle = "바뀐 코스",
                        courseImageUrl = "https://cdn/changed.jpg",
                    )

                notificationPersistencePort.upsert(latest)

                val rows = notificationPersistencePort.findPage(USER_ID, null, 10)
                assertThat(rows.map { it.id }).containsExactly(inserted.id)
                val updated = rows.single()
                assertThat(updated.type).isEqualTo(NotificationType.COURSE_LIKED)
                assertThat(updated.targetType).isEqualTo(NotificationTargetType.COURSE)
                assertThat(updated.targetId).isEqualTo(first.targetId)
                assertThat(updated.actorCount).isEqualTo(2)
                assertThat(updated.lastActorId).isEqualTo(latest.lastActorId)
                assertThat(updated.actorNickname).isEqualTo(latest.actorNickname)
                assertThat(updated.courseTitle).isEqualTo(latest.courseTitle)
                assertThat(updated.courseImageUrl).isEqualTo(latest.courseImageUrl)
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
                notificationPersistencePort.upsert(first)
                val firstId = notificationPersistencePort.findPage(USER_ID, null, 10).single().id
                notificationPersistencePort.upsert(first.copy(windowStart = first.windowStart.plusSeconds(600)))
                val secondId = notificationPersistencePort.findPage(USER_ID, null, 1).single().id
                notificationPersistencePort.upsert(first.copy(userId = OTHER_USER_ID))

                assertThat(
                    notificationPersistencePort.findPage(USER_ID, null, 10).map { it.id },
                ).containsExactly(secondId, firstId)
                assertThat(
                    notificationPersistencePort.findPage(USER_ID, secondId, 10).map { it.id },
                ).containsExactly(firstId)
                assertThat(notificationPersistencePort.findPage(USER_ID, firstId, 10)).isEmpty()
                assertThat(notificationPersistencePort.countUnread(USER_ID)).isEqualTo(2L)
                assertThat(notificationPersistencePort.countUnread(OTHER_USER_ID)).isEqualTo(1L)
                rollback()
            }
        }

        @Test
        fun `읽음 처리는 본인 미읽음만 갱신하고 반복과 없는 알림은 0을 반환한다`() {
            transaction {
                notificationPersistencePort.upsert(bundle())
                val id = notificationPersistencePort.findPage(USER_ID, null, 10).single().id

                assertThat(notificationPersistencePort.markRead(OTHER_USER_ID, id)).isZero()
                assertThat(notificationPersistencePort.markRead(USER_ID, Long.MAX_VALUE)).isZero()
                assertThat(notificationPersistencePort.countUnread(USER_ID)).isEqualTo(1L)
                assertThat(notificationPersistencePort.markRead(USER_ID, id)).isEqualTo(1)
                assertThat(notificationPersistencePort.markRead(USER_ID, id)).isZero()
                assertThat(notificationPersistencePort.countUnread(USER_ID)).isZero()
                assertThat(notificationPersistencePort.findPage(USER_ID, null, 10).single().isRead).isTrue()
                rollback()
            }
        }

        @Test
        fun `읽은 묶음에 다른 행동자가 기록되면 인원이 늘고 미읽음으로 돌아간다`() {
            transaction {
                val activity = bundle()
                notificationPersistencePort.upsert(activity)
                val id = notificationPersistencePort.findPage(USER_ID, null, 10).single().id
                notificationPersistencePort.markRead(USER_ID, id)

                notificationPersistencePort.upsert(activity.copy(lastActorId = 570_902L))

                val rows = notificationPersistencePort.findPage(USER_ID, null, 10)
                assertThat(rows.map { it.id }).containsExactly(id)
                assertThat(rows.single().actorCount).isEqualTo(2)
                assertThat(rows.single().isRead).isFalse()
                assertThat(notificationPersistencePort.countUnread(USER_ID)).isEqualTo(1L)
                rollback()
            }
        }

        @Test
        fun `이미 센 행동자가 다시 기록하면 인원과 읽음 상태와 스냅샷을 건드리지 않는다`() {
            transaction {
                val activity = bundle()
                notificationPersistencePort.upsert(activity)
                val id = notificationPersistencePort.findPage(USER_ID, null, 10).single().id
                notificationPersistencePort.markRead(USER_ID, id)
                val before = notificationPersistencePort.findPage(USER_ID, null, 10).single()

                notificationPersistencePort.upsert(activity.copy(courseTitle = "바뀐 코스"))

                assertThat(notificationPersistencePort.findPage(USER_ID, null, 10).single()).isEqualTo(before)
                assertThat(before.actorCount).isEqualTo(1)
                assertThat(before.isRead).isTrue()
                rollback()
            }
        }

        @Test
        fun `A 다음 B 다음 A 가 기록하면 서로 다른 사람 수인 2로 센다`() {
            transaction {
                val actorA = bundle()
                val actorB = actorA.copy(lastActorId = 570_902L)

                listOf(actorA, actorB, actorA).forEach { notificationPersistencePort.upsert(it) }

                val row = notificationPersistencePort.findPage(USER_ID, null, 10).single()
                assertThat(row.actorCount).isEqualTo(2)
                assertThat(row.lastActorId).isEqualTo(actorB.lastActorId)
                rollback()
            }
        }

        @Test
        fun `창이 지난 뒤 좋아요를 취소하고 다시 하면 알림은 새 창의 한 건만 남는다`() {
            transaction {
                val like = bundle()
                notificationPersistencePort.upsert(like)

                assertThat(notificationPersistencePort.removeActor(ACTOR_ID, like.type, like.targetId, null)).isEmpty()
                notificationPersistencePort.upsert(like.copy(windowStart = like.windowStart.plusSeconds(3600)))

                val row = notificationPersistencePort.findPage(USER_ID, null, 10).single()
                assertThat(row.actorCount).isEqualTo(1)
                assertThat(row.isRead).isFalse()
                rollback()
            }
        }

        @Test
        fun `행동자를 빼면 모든 창에서 인원을 줄이고 마지막 행동자를 바꾸며 다른 대상과 종류는 그대로 둔다`() {
            transaction {
                val review =
                    bundle().copy(
                        type = NotificationType.COURSE_REVIEWED,
                        lastActorId = OTHER_ACTOR_ID,
                        actorNickname = "남는 사람",
                    )
                notificationPersistencePort.upsert(review)
                notificationPersistencePort.upsert(review.copy(lastActorId = ACTOR_ID, actorNickname = "지운 사람"))
                notificationPersistencePort.upsert(
                    review.copy(lastActorId = ACTOR_ID, windowStart = review.windowStart.plusSeconds(600)),
                )
                notificationPersistencePort.upsert(review.copy(lastActorId = ACTOR_ID, targetId = 570_999L))
                notificationPersistencePort.upsert(bundle())
                val keptId = notificationPersistencePort.findPage(USER_ID, null, 10).last().id

                val changed = notificationPersistencePort.removeActor(ACTOR_ID, review.type, review.targetId, null)
                notificationPersistencePort.updateActorNickname(changed.keys.single(), "남는 사람")

                assertThat(changed).containsExactlyEntriesOf(mapOf(keptId to OTHER_ACTOR_ID))
                val rows = notificationPersistencePort.findPage(USER_ID, null, 10)
                assertThat(rows.map { it.type to it.targetId }).containsExactly(
                    NotificationType.COURSE_LIKED to review.targetId,
                    NotificationType.COURSE_REVIEWED to 570_999L,
                    NotificationType.COURSE_REVIEWED to review.targetId,
                )
                val kept = rows.single { it.id == keptId }
                assertThat(kept.actorCount).isEqualTo(1)
                assertThat(kept.lastActorId).isEqualTo(OTHER_ACTOR_ID)
                assertThat(kept.actorNickname).isEqualTo("남는 사람")
                rollback()
            }
        }

        @Test
        fun `받는 사람을 지정하면 그 사람의 묶음에서만 행동자를 뺀다`() {
            transaction {
                val follow =
                    bundle().copy(
                        type = NotificationType.USER_FOLLOWED,
                        targetType = NotificationTargetType.USER,
                        targetId = ACTOR_ID,
                        courseTitle = null,
                        courseImageUrl = null,
                    )
                notificationPersistencePort.upsert(follow)
                notificationPersistencePort.upsert(follow.copy(userId = OTHER_USER_ID))

                notificationPersistencePort.removeActor(ACTOR_ID, follow.type, ACTOR_ID, USER_ID)

                assertThat(notificationPersistencePort.findPage(USER_ID, null, 10)).isEmpty()
                assertThat(notificationPersistencePort.findPage(OTHER_USER_ID, null, 10)).hasSize(1)
                rollback()
            }
        }

        @Test
        fun `보관 기간 기준 시각 이전에 만든 알림만 지운다`() {
            transaction {
                notificationPersistencePort.upsert(bundle())
                notificationPersistencePort.upsert(bundle().copy(targetId = 2L))
                exec("UPDATE notifications SET created_at = now() - interval '91 days' WHERE target_id = 2")

                assertThat(
                    notificationPersistencePort.deleteCreatedBefore(Instant.now().minusSeconds(90L * 86_400)),
                ).isEqualTo(1)

                assertThat(
                    notificationPersistencePort.findPage(USER_ID, null, 10).map { it.targetId },
                ).containsExactly(570_901L)
                rollback()
            }
        }

        @Test
        fun `모두 읽음은 본인의 미읽음 수만 반환하고 다른 사용자는 유지한다`() {
            transaction {
                notificationPersistencePort.upsert(bundle())
                val readId = notificationPersistencePort.findPage(USER_ID, null, 10).single().id
                notificationPersistencePort.markRead(USER_ID, readId)
                notificationPersistencePort.upsert(bundle().copy(targetId = 2L))
                notificationPersistencePort.upsert(bundle().copy(targetId = 3L))
                notificationPersistencePort.upsert(bundle().copy(userId = OTHER_USER_ID))

                assertThat(notificationPersistencePort.markAllRead(USER_ID)).isEqualTo(2)
                assertThat(notificationPersistencePort.markAllRead(USER_ID)).isZero()
                assertThat(notificationPersistencePort.countUnread(USER_ID)).isZero()
                assertThat(notificationPersistencePort.countUnread(OTHER_USER_ID)).isEqualTo(1L)
                assertThat(
                    notificationPersistencePort.findPage(USER_ID, null, 10).map { it.isRead },
                ).containsExactly(true, true, true)
                rollback()
            }
        }

        @Test
        fun `설정 행이 없으면 null을 반환하고 부분 업서트는 나머지 기본값을 유지한다`() {
            transaction {
                assertThat(notificationSettingsPersistencePort.findByUser(USER_ID)).isNull()

                val saved =
                    notificationSettingsPersistencePort.update(
                        USER_ID,
                        UpdateNotificationSettingsCommand(activityLike = false),
                    )

                assertThat(saved).isEqualTo(NotificationSettings.DEFAULT.copy(activityLike = false))
                assertThat(notificationSettingsPersistencePort.findByUser(USER_ID)).isEqualTo(saved)
                assertThat(notificationSettingsPersistencePort.findByUser(OTHER_USER_ID)).isNull()
                rollback()
            }
        }

        @Test
        fun `설정 부분 수정의 null은 기존값을 유지하고 모든 카테고리를 따로 수정한다`() {
            transaction {
                notificationSettingsPersistencePort.update(
                    USER_ID,
                    UpdateNotificationSettingsCommand(activityLike = false, activityFollow = false),
                )

                val saved =
                    notificationSettingsPersistencePort.update(
                        USER_ID,
                        UpdateNotificationSettingsCommand(
                            activityComment = false,
                            activityReview = false,
                        ),
                    )

                assertThat(saved).isEqualTo(NotificationSettings(false, false, false, false))
                assertThat(
                    notificationSettingsPersistencePort.update(USER_ID, UpdateNotificationSettingsCommand()),
                ).isEqualTo(saved)
                assertThat(
                    notificationSettingsPersistencePort.update(
                        USER_ID,
                        UpdateNotificationSettingsCommand(activityLike = true),
                    ),
                ).isEqualTo(saved.copy(activityLike = true))
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
                lastActorId = ACTOR_ID,
                actorNickname = null,
                courseTitle = "알림 코스",
                courseImageUrl = "https://cdn/course.jpg",
            )

        private companion object {
            const val USER_ID = 570_901L
            const val OTHER_USER_ID = 570_902L
            const val ACTOR_ID = 570_900L
            const val OTHER_ACTOR_ID = 570_903L
        }
    }
