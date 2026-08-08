package com.example.backend.course.adapter.outbound

import com.example.backend.course.application.port.outbound.AuthorCourseCountPort
import com.example.backend.user.application.port.inbound.UserCourseCountUseCase
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [AuthorCourseCountPort] 를 user 도메인 인바운드 포트에 위임한다(인프로세스).
 * course 애플리케이션이 user 를 직접 알지 않도록 하는 통합 경계 지점. MSA 분리 시 이 어댑터만
 * user 서비스 클라이언트로 교체한다(그때 동기 호출 → outbox/메시지·결과적 일관성으로 전환).
 */
@Component
class AuthorCourseCountAdapter(
    private val userCourseCountUseCase: UserCourseCountUseCase,
) : AuthorCourseCountPort {
    override fun applyDelta(
        authorId: Long,
        publicDelta: Int,
        followerDelta: Int,
        privateDelta: Int,
    ) = userCourseCountUseCase.applyCourseCountDelta(authorId, publicDelta, followerDelta, privateDelta)
}
