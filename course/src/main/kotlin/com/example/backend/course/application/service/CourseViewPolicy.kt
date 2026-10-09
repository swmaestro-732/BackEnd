package com.example.backend.course.application.service

import com.example.backend.common.domain.CourseVisibility
import com.example.backend.course.application.port.outbound.ViewerInteractionPort
import org.springframework.stereotype.Component

/**
 * 코스 열람 권한 정책 — 공개범위(와 팔로우 관계)로 조회자가 코스를 볼 수 있는지 판정한다.
 * PUBLIC 은 전체, FOLLOWER 는 소유자·팔로워, PRIVATE 은 소유자만.
 * 조회(상세·목록)와 좋아요 등 "열람 권한이 있어야 하는" 경로에서 같은 규칙을 공용으로 쓴다.
 * 팔로우 관계는 user 도메인 소유라 [ViewerInteractionPort](아웃바운드)로만 확인한다.
 */
@Component
class CourseViewPolicy(
    private val viewerInteractionPort: ViewerInteractionPort,
) {
    fun isViewable(
        visibility: CourseVisibility,
        ownerId: Long,
        viewerId: Long?,
    ): Boolean =
        when (visibility) {
            CourseVisibility.PUBLIC -> {
                true
            }

            CourseVisibility.FOLLOWER -> {
                viewerId == ownerId ||
                    (viewerId != null && viewerInteractionPort.isFollowing(viewerId, ownerId))
            }

            CourseVisibility.PRIVATE -> {
                viewerId == ownerId
            }
        }
}
