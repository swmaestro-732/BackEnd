package com.example.backend.user.application.service

import com.example.backend.user.application.port.inbound.CourseInteractionUseCase
import com.example.backend.user.application.port.inbound.dto.CourseViewerState
import com.example.backend.user.application.port.outbound.CourseInteractionPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CourseInteractionService(
    private val courseInteractionPort: CourseInteractionPort,
) : CourseInteractionUseCase {
    override fun getViewerState(
        userId: Long,
        courseId: Long,
    ): CourseViewerState =
        CourseViewerState(
            hasSaved = courseInteractionPort.existsSavedCourse(userId, courseId),
            hasStartedCourse = courseInteractionPort.existsTracingCourse(userId, courseId),
        )
}
