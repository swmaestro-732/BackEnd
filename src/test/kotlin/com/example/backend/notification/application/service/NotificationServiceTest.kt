package com.example.backend.notification.application.service

import com.example.backend.notification.application.port.inbound.dto.NotificationSummary
import com.example.backend.notification.application.port.inbound.dto.RecordNotificationCommand
import com.example.backend.notification.application.port.outbound.NotificationActor
import com.example.backend.notification.application.port.outbound.NotificationActorLookupPort
import com.example.backend.notification.application.port.outbound.NotificationCourseTarget
import com.example.backend.notification.application.port.outbound.NotificationPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationSettingsPersistencePort
import com.example.backend.notification.application.port.outbound.NotificationTargetLookupPort
import com.example.backend.notification.domain.model.NotificationBundle
import com.example.backend.notification.domain.model.NotificationCategory
import com.example.backend.notification.domain.model.NotificationPayload
import com.example.backend.notification.domain.model.NotificationSettings
import com.example.backend.notification.domain.model.NotificationTargetType
import com.example.backend.notification.domain.model.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant

class NotificationServiceTest {
    private val targets = mock(NotificationTargetLookupPort::class.java)
    private val actors = mock(NotificationActorLookupPort::class.java)
    private val notifications = mock(NotificationPersistencePort::class.java)
    private val settings = mock(NotificationSettingsPersistencePort::class.java)
    private val service = NotificationService(targets, actors, notifications, settings)

    @ParameterizedTest
    @EnumSource(value = NotificationType::class, names = ["USER_FOLLOWED"], mode = EnumSource.Mode.EXCLUDE)
    fun `코스 활동은 작성자와 행위자 스냅샷을 10분 묶음으로 기록한다`(type: NotificationType) {
        `when`(targets.findCourse(COURSE_ID)).thenReturn(NotificationCourseTarget(OWNER_ID, "한강 산책"))
        `when`(settings.findByUser(OWNER_ID)).thenReturn(null)
        `when`(actors.findActor(ACTOR_ID)).thenReturn(NotificationActor("산책자", "https://example.com/profile.jpg"))

        service.record(command(type))

        val order = inOrder(targets, settings, actors, notifications)
        order.verify(targets).findCourse(COURSE_ID)
        order.verify(settings).findByUser(OWNER_ID)
        order.verify(actors).findActor(ACTOR_ID)
        order.verify(notifications).upsert(
            NotificationBundle(
                userId = OWNER_ID,
                type = type,
                targetType = NotificationTargetType.COURSE,
                targetId = COURSE_ID,
                windowStart = WINDOW_START,
                lastActorId = ACTOR_ID,
                payload = NotificationPayload("산책자", "https://example.com/profile.jpg", "한강 산책"),
            ),
        )
    }

    @Test
    fun `팔로우는 대상 사용자에게 코스 제목 없이 기록한다`() {
        `when`(settings.findByUser(OWNER_ID)).thenReturn(NotificationSettings.DEFAULT)
        `when`(actors.findActor(ACTOR_ID)).thenReturn(NotificationActor("새 팔로워", null))

        service.record(command(NotificationType.USER_FOLLOWED, targetId = OWNER_ID))

        verifyNoInteractions(targets)
        verify(settings).findByUser(OWNER_ID)
        verify(notifications).upsert(
            NotificationBundle(
                userId = OWNER_ID,
                type = NotificationType.USER_FOLLOWED,
                targetType = NotificationTargetType.USER,
                targetId = OWNER_ID,
                windowStart = WINDOW_START,
                lastActorId = ACTOR_ID,
                payload = NotificationPayload("새 팔로워", null),
            ),
        )
    }

    @ParameterizedTest
    @EnumSource(value = NotificationType::class, names = ["USER_FOLLOWED"], mode = EnumSource.Mode.EXCLUDE)
    fun `대상 코스가 없으면 기록하지 않는다`(type: NotificationType) {
        `when`(targets.findCourse(COURSE_ID)).thenReturn(null)

        service.record(command(type))

        verifyNoInteractions(settings, actors, notifications)
    }

    @ParameterizedTest
    @EnumSource(value = NotificationType::class, names = ["USER_FOLLOWED"], mode = EnumSource.Mode.EXCLUDE)
    fun `자기 코스에 한 행동은 기록하지 않는다`(type: NotificationType) {
        `when`(targets.findCourse(COURSE_ID)).thenReturn(NotificationCourseTarget(ACTOR_ID, "내 코스"))

        service.record(command(type))

        verifyNoInteractions(settings, actors, notifications)
    }

    @Test
    fun `자기 자신을 팔로우한 이벤트는 기록하지 않는다`() {
        service.record(command(NotificationType.USER_FOLLOWED, targetId = ACTOR_ID))

        verifyNoInteractions(targets, settings, actors, notifications)
    }

    @ParameterizedTest
    @EnumSource(NotificationType::class)
    fun `받는 사람이 해당 활동 알림을 껐으면 기록하지 않는다`(type: NotificationType) {
        `when`(targets.findCourse(COURSE_ID)).thenReturn(NotificationCourseTarget(OWNER_ID, "한강 산책"))
        `when`(settings.findByUser(OWNER_ID)).thenReturn(
            NotificationSettings(
                activityLike = type.category != NotificationCategory.LIKE,
                activityComment = type.category != NotificationCategory.COMMENT,
                activityFollow = type.category != NotificationCategory.FOLLOW,
                activityCourse = type.category != NotificationCategory.COURSE,
                activityReview = type.category != NotificationCategory.REVIEW,
            ),
        )

        service.record(command(type, targetId = if (type == NotificationType.USER_FOLLOWED) OWNER_ID else COURSE_ID))

        verify(settings).findByUser(OWNER_ID)
        verifyNoInteractions(actors, notifications)
    }

    @Test
    fun `행위자가 탈퇴해 조회되지 않으면 기록하지 않는다`() {
        `when`(targets.findCourse(COURSE_ID)).thenReturn(NotificationCourseTarget(OWNER_ID, "한강 산책"))
        `when`(actors.findActor(ACTOR_ID)).thenReturn(null)

        service.record(command(NotificationType.COURSE_LIKED))

        verify(actors).findActor(ACTOR_ID)
        verifyNoInteractions(notifications)
    }

    @Test
    fun `한 행 더 조회해 다음 페이지가 있으면 마지막 항목을 커서로 반환한다`() {
        val first = summary(30L)
        val second = summary(20L)
        `when`(notifications.findPage(OWNER_ID, null, 3)).thenReturn(listOf(first, second, summary(10L)))

        val result = service.list(OWNER_ID, null, 2)

        assertThat(result.items).containsExactly(first, second)
        assertThat(result.hasNext).isTrue()
        assertThat(result.nextCursor).isEqualTo(20L)
        verify(notifications).findPage(OWNER_ID, null, 3)
    }

    @Test
    fun `요청 개수와 같은 마지막 페이지는 다음 커서가 없다`() {
        val last = summary(10L)
        `when`(notifications.findPage(OWNER_ID, 20L, 2)).thenReturn(listOf(last))

        val result = service.list(OWNER_ID, 20L, 1)

        assertThat(result.items).containsExactly(last)
        assertThat(result.hasNext).isFalse()
        assertThat(result.nextCursor).isNull()
        verify(notifications).findPage(OWNER_ID, 20L, 2)
    }

    @Test
    fun `알림이 없으면 빈 마지막 페이지를 반환한다`() {
        `when`(notifications.findPage(OWNER_ID, null, 21)).thenReturn(emptyList())

        val result = service.list(OWNER_ID, null, 20)

        assertThat(result.items).isEmpty()
        assertThat(result.hasNext).isFalse()
        assertThat(result.nextCursor).isNull()
    }

    @Test
    fun `목록 크기는 최소 한 개로 보정하고 추가 행을 조회한다`() {
        `when`(notifications.findPage(OWNER_ID, null, 2)).thenReturn(listOf(summary(20L), summary(10L)))

        val result = service.list(OWNER_ID, null, 0)

        assertThat(result.items.map { it.id }).containsExactly(20L)
        assertThat(result.hasNext).isTrue()
        assertThat(result.nextCursor).isEqualTo(20L)
        verify(notifications).findPage(OWNER_ID, null, 2)
    }

    @Test
    fun `목록 크기는 최대 50개로 보정하고 추가 행을 조회한다`() {
        `when`(notifications.findPage(OWNER_ID, null, 51)).thenReturn(emptyList())

        service.list(OWNER_ID, null, Int.MAX_VALUE)

        verify(notifications).findPage(OWNER_ID, null, 51)
    }

    private fun summary(id: Long) =
        NotificationSummary(
            id = id,
            type = NotificationType.COURSE_LIKED,
            targetType = NotificationTargetType.COURSE,
            targetId = COURSE_ID,
            actorCount = 1,
            lastActorId = ACTOR_ID,
            actorNickname = "산책자",
            actorProfileImageUrl = null,
            courseTitle = "한강 산책",
            isRead = false,
            createdAt = OCCURRED_AT,
            updatedAt = OCCURRED_AT,
        )

    private fun command(
        type: NotificationType,
        targetId: Long = COURSE_ID,
    ) = RecordNotificationCommand(type, ACTOR_ID, targetId, OCCURRED_AT)

    private companion object {
        const val OWNER_ID = 1L
        const val ACTOR_ID = 2L
        const val COURSE_ID = 10L
        val OCCURRED_AT: Instant = Instant.parse("2026-10-24T09:59:59.999Z")
        val WINDOW_START: Instant = Instant.parse("2026-10-24T09:50:00Z")
    }
}
