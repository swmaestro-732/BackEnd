package com.example.backend.notification.adapter.inbound.event

import com.example.backend.bootstrap.config.AsyncConfig
import com.example.backend.course.application.port.inbound.CourseActivityKind
import com.example.backend.course.application.port.inbound.CourseActivityOccurred
import com.example.backend.notification.application.port.inbound.NotificationUseCase
import com.example.backend.notification.application.port.inbound.dto.RecordNotificationCommand
import com.example.backend.notification.domain.model.NotificationType
import com.example.backend.user.application.port.inbound.UserActivityKind
import com.example.backend.user.application.port.inbound.UserActivityOccurred
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/** 발행 도메인의 공개 계약만 받아 커밋 이후 알림 기록을 시도한다. */
@Component
class NotificationEventListener(
    private val useCase: NotificationUseCase,
    private val meterRegistry: MeterRegistry,
) {
    private val log = KotlinLogging.logger {}

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCourseActivity(event: CourseActivityOccurred) {
        try {
            useCase.record(
                RecordNotificationCommand(courseType(event.kind), event.actorId, event.courseId, event.occurredAt),
            )
        } catch (e: Exception) {
            log.warn(e) { "코스 활동 알림 기록 실패: type=${event.kind}" }
            meterRegistry.counter("notification.record.failed", "type", courseType(event.kind).name).increment()
        }
    }

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onUserActivity(event: UserActivityOccurred) {
        try {
            useCase.record(
                RecordNotificationCommand(userType(event.kind), event.actorId, event.targetId, event.occurredAt),
            )
        } catch (e: Exception) {
            log.warn(e) { "사용자 활동 알림 기록 실패: type=${event.kind}" }
            meterRegistry.counter("notification.record.failed", "type", userType(event.kind).name).increment()
        }
    }

    private fun courseType(kind: CourseActivityKind): NotificationType =
        when (kind) {
            CourseActivityKind.LIKED -> NotificationType.COURSE_LIKED
            CourseActivityKind.COMMENTED -> NotificationType.COURSE_COMMENTED
            CourseActivityKind.TRACE_COMPLETED -> NotificationType.COURSE_TRACE_COMPLETED
            CourseActivityKind.REVIEWED -> NotificationType.COURSE_REVIEWED
        }

    private fun userType(kind: UserActivityKind): NotificationType =
        when (kind) {
            UserActivityKind.FOLLOWED -> NotificationType.USER_FOLLOWED
            UserActivityKind.COURSE_BOOKMARKED -> NotificationType.COURSE_BOOKMARKED
        }
}
