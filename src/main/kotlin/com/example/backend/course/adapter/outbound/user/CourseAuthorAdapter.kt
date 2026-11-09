package com.example.backend.course.adapter.outbound.user

import com.example.backend.course.application.port.outbound.CourseAuthorPort
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import org.springframework.stereotype.Component

/** ACL 어댑터: [CourseAuthorPort] 를 user 도메인 인바운드 포트([UserSummaryUseCase])로 위임한다. */
@Component
class CourseAuthorAdapter(
    private val userSummaryUseCase: UserSummaryUseCase,
) : CourseAuthorPort {
    override fun findNicknames(userIds: Collection<Long>): Map<Long, String> =
        userSummaryUseCase.findSummaries(userIds).associate { it.id to it.nickname }

    override fun findIdsByNickname(
        keyword: String,
        limit: Int,
    ): List<Long> = userSummaryUseCase.findIdsByNickname(keyword, limit)
}
