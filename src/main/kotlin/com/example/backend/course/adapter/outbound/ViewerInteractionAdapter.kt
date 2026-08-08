package com.example.backend.course.adapter.outbound

import com.example.backend.course.application.port.outbound.ViewerCourseState
import com.example.backend.course.application.port.outbound.ViewerInteractionPort
import com.example.backend.user.application.port.inbound.CourseInteractionUseCase
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [ViewerInteractionPort] 를 user 도메인 인바운드 포트([CourseInteractionUseCase])에 위임하고,
 * user 응답을 course 격리 DTO 로 매핑한다(인프로세스). course 애플리케이션이 user 를 직접 알지 않게 하는 경계 지점.
 * MSA 분리 시 이 어댑터만 user 서비스 클라이언트로 교체한다.
 */
@Component
class ViewerInteractionAdapter(
    private val courseInteractionUseCase: CourseInteractionUseCase,
) : ViewerInteractionPort {
    override fun getViewerStates(
        viewerId: Long,
        courseIds: List<Long>,
    ): List<ViewerCourseState> =
        courseInteractionUseCase.getViewerStates(viewerId, courseIds).map {
            ViewerCourseState(
                courseId = it.courseId,
                hasSaved = it.hasSaved,
                hasStartedCourse = it.hasStartedCourse,
            )
        }

    override fun isFollowing(
        followerId: Long,
        followingId: Long,
    ): Boolean = courseInteractionUseCase.isFollowing(followerId, followingId)
}
