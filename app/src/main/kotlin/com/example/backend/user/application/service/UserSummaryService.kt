package com.example.backend.user.application.service

import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import com.example.backend.user.application.port.outbound.UserSummaryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** [UserSummaryUseCase] 구현 — 다른 도메인이 쓰는 사용자 표시 정보 조회. */
@Service
@Transactional(readOnly = true)
class UserSummaryService(
    private val userSummaryPort: UserSummaryPort,
) : UserSummaryUseCase {
    override fun findSummaries(ids: Collection<Long>): List<UserSummaryUseCase.UserSummary> {
        if (ids.isEmpty()) return emptyList()
        return userSummaryPort.findByIds(ids)
    }
}
