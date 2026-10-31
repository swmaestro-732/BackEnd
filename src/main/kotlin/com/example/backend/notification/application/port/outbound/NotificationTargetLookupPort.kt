package com.example.backend.notification.application.port.outbound

data class NotificationCourseTarget(
    val authorId: Long,
    val title: String,
)

interface NotificationTargetLookupPort {
    fun findCourse(courseId: Long): NotificationCourseTarget?
}
