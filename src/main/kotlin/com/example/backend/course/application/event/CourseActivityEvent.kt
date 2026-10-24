package com.example.backend.course.application.event

import com.example.backend.course.application.port.inbound.CourseActivityKind
import com.example.backend.course.application.port.inbound.CourseActivityOccurred
import java.time.Instant

data class CourseActivityEvent(
    override val kind: CourseActivityKind,
    override val actorId: Long,
    override val courseId: Long,
    override val occurredAt: Instant,
) : CourseActivityOccurred
