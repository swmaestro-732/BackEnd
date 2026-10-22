package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.UserRepository
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import com.example.backend.user.application.port.outbound.UserSummaryPort
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [UserSummaryPort] 를 구현한다. 실제 조회는 [UserRepository] 에 위임한다. */
@Component
class UserSummaryPersistenceAdapter(
    private val userRepository: UserRepository,
) : UserSummaryPort {
    override fun findByIds(ids: Collection<Long>): List<UserSummaryUseCase.UserSummary> =
        userRepository.findSummariesByIds(ids)
}
