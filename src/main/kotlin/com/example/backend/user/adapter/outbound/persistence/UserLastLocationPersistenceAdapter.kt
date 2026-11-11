package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.adapter.outbound.persistence.exposed.repository.UserLastLocationRepository
import com.example.backend.user.application.port.outbound.UserLastLocationPersistencePort
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터. [UserLastLocationPersistencePort] 를 구현하고 테이블 접근은 [UserLastLocationRepository] 에 위임한다. */
@Component
class UserLastLocationPersistenceAdapter(
    private val userLastLocationRepository: UserLastLocationRepository,
) : UserLastLocationPersistencePort {
    override fun isRecentNear(
        userId: Long,
        location: Coordinate,
    ): Boolean = userLastLocationRepository.isRecentNear(userId, location)

    override fun save(
        userId: Long,
        location: Coordinate,
    ) = userLastLocationRepository.upsert(userId, location)

    override fun deleteByUser(userId: Long) = userLastLocationRepository.deleteByUserId(userId)
}
