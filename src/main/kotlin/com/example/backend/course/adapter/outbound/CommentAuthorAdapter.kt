package com.example.backend.course.adapter.outbound

import com.example.backend.course.application.port.outbound.CommentAuthorPort
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [CommentAuthorPort] 구현. user 도메인 인바운드 포트([UserSummaryUseCase])에 위임한다. */
@Component
class CommentAuthorAdapter(
    private val userSummaryUseCase: UserSummaryUseCase,
) : CommentAuthorPort {
    override fun findNicknames(userIds: Collection<Long>): Map<Long, String> =
        userSummaryUseCase.findSummaries(userIds).associate { it.id to it.nickname }
}
