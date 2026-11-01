package com.example.backend.notification.adapter.outbound.lookup

import com.example.backend.course.application.port.inbound.CourseQueryUseCase
import com.example.backend.notification.application.port.outbound.NotificationCourseTarget
import com.example.backend.notification.application.port.outbound.NotificationTargetLookupPort
import org.springframework.stereotype.Component

@Component
class NotificationTargetLookupAdapter(
    private val courseQueryUseCase: CourseQueryUseCase,
) : NotificationTargetLookupPort {
    override fun findCourse(courseId: Long): NotificationCourseTarget? =
        courseQueryUseCase.findOwnerSummary(courseId)?.let {
            NotificationCourseTarget(it.authorId, it.title)
        }
}
