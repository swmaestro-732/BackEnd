package com.example.backend.user.application.service

import com.example.backend.user.application.port.inbound.CourseInteractionUseCase
import com.example.backend.user.application.port.inbound.dto.CourseViewerState
import com.example.backend.user.application.port.outbound.CourseInteractionPort
import com.example.backend.user.application.port.outbound.FollowPersistencePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CourseInteractionService(
    private val courseInteractionPort: CourseInteractionPort,
    private val followPersistencePort: FollowPersistencePort,
) : CourseInteractionUseCase {
    override fun getViewerStates(
        userId: Long,
        courseIds: List<Long>,
    ): List<CourseViewerState> {
        if (courseIds.isEmpty()) return emptyList()
        val saved = courseInteractionPort.findSavedCourseIds(userId, courseIds)
        val started = courseInteractionPort.findStartedCourseIds(userId, courseIds)
        val completedAt = courseInteractionPort.findCompletedAt(userId, courseIds)
        return courseIds.map { courseId ->
            CourseViewerState(
                courseId = courseId,
                hasSaved = courseId in saved,
                hasStarted = courseId in started,
                completedAt = completedAt[courseId],
            )
        }
    }

    override fun isFollowing(
        followerId: Long,
        followingId: Long,
    ): Boolean = followPersistencePort.isFollowing(followerId, followingId)
}
