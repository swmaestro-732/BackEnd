package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.UserAreaRepository
import com.example.backend.user.application.port.outbound.UserAreaPersistencePort
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [UserAreaPersistencePort] 를 구현한다. 실제 테이블 접근은 [UserAreaRepository] 에 위임한다. */
@Component
class UserAreaPersistenceAdapter(
    private val userAreaRepository: UserAreaRepository,
) : UserAreaPersistencePort {
    override fun replaceAreas(
        userId: Long,
        areaCodes: List<String>,
    ) = userAreaRepository.replaceByUserId(userId, areaCodes)

    override fun findAreaCodes(userId: Long): List<String> = userAreaRepository.findByUserId(userId)
}
