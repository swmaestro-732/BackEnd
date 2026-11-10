package com.example.backend.course.application.event

import com.example.backend.course.application.port.inbound.CourseModeratedByAdmin
import com.example.backend.course.application.port.inbound.CourseModerationAction
import java.time.Instant

/** 도메인 이벤트: 어드민의 사용자 코스 숨김, 삭제([CourseModeratedByAdmin] 계약). */
data class CourseModeratedEvent(
    override val authorId: Long,
    override val courseId: Long,
    override val courseTitle: String,
    override val coverImageUrl: String?,
    override val action: CourseModerationAction,
    override val occurredAt: Instant,
) : CourseModeratedByAdmin
