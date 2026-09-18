package com.example.backend.course.adapter.outbound

import com.example.backend.course.adapter.outbound.persistence.exposed.repository.CourseLikeRepository
import com.example.backend.course.application.port.outbound.ViewerCourseState
import com.example.backend.course.application.port.outbound.ViewerInteractionPort
import com.example.backend.user.application.port.inbound.CourseInteractionUseCase
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [ViewerInteractionPort] 구현. 저장·완주 상태와 팔로우 여부는 user 도메인 인바운드 포트
 * ([CourseInteractionUseCase])에 위임하고, 좋아요 여부는 course 로컬 [CourseLikeRepository] 에서 읽어 합친다.
 * MSA 분리 시 user 위임 부분만 서비스 클라이언트로 교체한다.
 */
@Component
class ViewerInteractionAdapter(
    private val courseInteractionUseCase: CourseInteractionUseCase,
    private val courseLikeRepository: CourseLikeRepository,
) : ViewerInteractionPort {
    override fun getViewerStates(
        viewerId: Long,
        courseIds: List<Long>,
    ): List<ViewerCourseState> {
        val liked = courseLikeRepository.findLikedCourseIds(viewerId, courseIds)
        return courseInteractionUseCase.getViewerStates(viewerId, courseIds).map {
            ViewerCourseState(
                courseId = it.courseId,
                hasSaved = it.hasSaved,
                hasLiked = it.courseId in liked,
                hasStartedCourse = it.hasStartedCourse,
            )
        }
    }

    override fun isFollowing(
        followerId: Long,
        followingId: Long,
    ): Boolean = courseInteractionUseCase.isFollowing(followerId, followingId)
}
