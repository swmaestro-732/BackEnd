package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.common.geo.Coordinate
import com.example.backend.user.adapter.outbound.persistence.exposed.UserLastLocationTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert
import org.springframework.stereotype.Repository

/** user_last_locations 테이블 접근 리포지토리. 사용자당 한 행을 덮어쓴다. */
@Repository
class UserLastLocationRepository {
    fun upsert(
        userId: Long,
        location: Coordinate,
    ) {
        UserLastLocationTable.upsert(
            UserLastLocationTable.userId,
            onUpdate = {
                it[UserLastLocationTable.location] = insertValue(UserLastLocationTable.location)
                it[UserLastLocationTable.updatedAt] = CurrentTimestamp
            },
        ) {
            it[UserLastLocationTable.userId] = userId
            it[UserLastLocationTable.location] = location
        }
    }

    fun findByUserId(userId: Long): Coordinate? =
        UserLastLocationTable
            .select(UserLastLocationTable.location)
            .where { UserLastLocationTable.userId eq userId }
            .singleOrNull()
            ?.get(UserLastLocationTable.location)

    fun deleteByUserId(userId: Long) {
        UserLastLocationTable.deleteWhere { UserLastLocationTable.userId eq userId }
    }
}
