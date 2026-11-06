package com.example.backend.notification.adapter.inbound.event

import com.example.backend.course.application.port.inbound.CourseActivityKind
import com.example.backend.course.application.port.inbound.CourseActivityOccurred
import com.example.backend.notification.application.port.inbound.NotificationUseCase
import com.example.backend.notification.application.port.inbound.dto.RecordNotificationCommand
import com.example.backend.notification.domain.model.NotificationType
import com.example.backend.user.application.port.inbound.UserActivityKind
import com.example.backend.user.application.port.inbound.UserActivityOccurred
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import java.time.Instant

class NotificationEventListenerTest {
    private val notificationUseCase = mock(NotificationUseCase::class.java)
    private val meterRegistry = SimpleMeterRegistry()
    private val listener = NotificationEventListener(notificationUseCase, meterRegistry)

    @ParameterizedTest
    @CsvSource(
        "LIKED, COURSE_LIKED",
        "COMMENTED, COURSE_COMMENTED",
        "REVIEWED, COURSE_REVIEWED",
    )
    fun `코스 활동 계약을 알림 명령으로 변환한다`(
        kind: CourseActivityKind,
        type: NotificationType,
    ) {
        listener.onCourseActivity(courseEvent(kind))

        verify(notificationUseCase).record(RecordNotificationCommand(type, ACTOR_ID, TARGET_ID, OCCURRED_AT))
        assertThat(meterRegistry.find("notification.record.failed").counters().sumOf { it.count() }).isZero()
    }

    @ParameterizedTest
    @CsvSource("FOLLOWED, USER_FOLLOWED")
    fun `사용자 활동 계약을 알림 명령으로 변환한다`(
        kind: UserActivityKind,
        type: NotificationType,
    ) {
        listener.onUserActivity(userEvent(kind))

        verify(notificationUseCase).record(RecordNotificationCommand(type, ACTOR_ID, TARGET_ID, OCCURRED_AT))
        assertThat(meterRegistry.find("notification.record.failed").counters().sumOf { it.count() }).isZero()
    }

    @ParameterizedTest
    @CsvSource("UNLIKED, COURSE_LIKED", "REVIEW_DELETED, COURSE_REVIEWED")
    fun `코스 취소 활동은 알림 철회로 변환한다`(
        kind: CourseActivityKind,
        type: NotificationType,
    ) {
        listener.onCourseActivity(courseEvent(kind))

        verify(notificationUseCase).retract(RecordNotificationCommand(type, ACTOR_ID, TARGET_ID, OCCURRED_AT))
        verifyNoMoreInteractions(notificationUseCase)
    }

    @Test
    fun `언팔로우는 알림 철회로 변환하고 실패하면 종류별 실패를 센다`() {
        val command = RecordNotificationCommand(NotificationType.USER_FOLLOWED, ACTOR_ID, TARGET_ID, OCCURRED_AT)
        doThrow(IllegalStateException("철회 실패")).`when`(notificationUseCase).retract(command)

        assertDoesNotThrow { listener.onUserActivity(userEvent(UserActivityKind.UNFOLLOWED)) }

        verify(notificationUseCase).retract(command)
        verifyNoMoreInteractions(notificationUseCase)
        assertThat(
            meterRegistry
                .get("notification.record.failed")
                .tag("type", "USER_FOLLOWED")
                .counter()
                .count(),
        ).isEqualTo(1.0)
    }

    @Test
    fun `시작 시 모든 알림 종류의 실패 카운터를 0으로 등록한다`() {
        val counters = meterRegistry.find("notification.record.failed").counters()

        assertThat(counters.map { it.id.getTag("type") })
            .containsExactlyInAnyOrderElementsOf(NotificationType.entries.map { it.name })
        assertThat(counters.sumOf { it.count() }).isZero()
    }

    @Test
    fun `코스 알림 기록이 실패해도 예외를 전파하지 않고 종류별 실패를 센다`() {
        val command = RecordNotificationCommand(NotificationType.COURSE_LIKED, ACTOR_ID, TARGET_ID, OCCURRED_AT)
        doThrow(IllegalStateException("알림 저장 실패")).`when`(notificationUseCase).record(command)

        assertDoesNotThrow { listener.onCourseActivity(courseEvent(CourseActivityKind.LIKED)) }

        verify(notificationUseCase).record(command)
        assertThat(
            meterRegistry
                .get("notification.record.failed")
                .tag("type", "COURSE_LIKED")
                .counter()
                .count(),
        ).isEqualTo(1.0)
    }

    @Test
    fun `사용자 알림 기록이 실패해도 예외를 전파하지 않고 종류별 실패를 센다`() {
        val command = RecordNotificationCommand(NotificationType.USER_FOLLOWED, ACTOR_ID, TARGET_ID, OCCURRED_AT)
        doThrow(IllegalStateException("알림 저장 실패")).`when`(notificationUseCase).record(command)

        assertDoesNotThrow { listener.onUserActivity(userEvent(UserActivityKind.FOLLOWED)) }

        verify(notificationUseCase).record(command)
        assertThat(
            meterRegistry
                .get("notification.record.failed")
                .tag("type", "USER_FOLLOWED")
                .counter()
                .count(),
        ).isEqualTo(1.0)
    }

    private fun courseEvent(kind: CourseActivityKind): CourseActivityOccurred =
        object : CourseActivityOccurred {
            override val kind = kind
            override val actorId = ACTOR_ID
            override val courseId = TARGET_ID
            override val occurredAt = OCCURRED_AT
        }

    private fun userEvent(kind: UserActivityKind): UserActivityOccurred =
        object : UserActivityOccurred {
            override val kind = kind
            override val actorId = ACTOR_ID
            override val targetId = TARGET_ID
            override val occurredAt = OCCURRED_AT
        }

    private companion object {
        const val ACTOR_ID = 2L
        const val TARGET_ID = 10L
        val OCCURRED_AT: Instant = Instant.parse("2026-10-24T09:59:59.999Z")
    }
}
