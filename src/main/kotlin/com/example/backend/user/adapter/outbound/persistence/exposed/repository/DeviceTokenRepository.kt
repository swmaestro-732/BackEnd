package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.DeviceTokenTable
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.upsert
import org.springframework.stereotype.Repository

@Repository
class DeviceTokenRepository {
    /** token 유니크 충돌 시 소유자와 헤더 값을 새 요청으로 덮어쓴다(계정 전환 시 소유자 이전). */
    fun upsert(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    ) {
        DeviceTokenTable.upsert(
            DeviceTokenTable.token,
            onUpdate = {
                it[DeviceTokenTable.userId] = insertValue(DeviceTokenTable.userId)
                it[DeviceTokenTable.platform] = insertValue(DeviceTokenTable.platform)
                it[DeviceTokenTable.appBuild] = insertValue(DeviceTokenTable.appBuild)
                it[DeviceTokenTable.lastSeenAt] = CurrentTimestamp
            },
        ) {
            it[DeviceTokenTable.userId] = userId
            it[DeviceTokenTable.token] = command.token
            it[DeviceTokenTable.platform] = command.platform
            it[DeviceTokenTable.appBuild] = command.appBuild
        }
    }

    fun deleteByUserAndToken(
        userId: Long,
        token: String,
    ) {
        DeviceTokenTable.deleteWhere { (DeviceTokenTable.userId eq userId) and (DeviceTokenTable.token eq token) }
    }

    fun deleteAllByUser(userId: Long) {
        DeviceTokenTable.deleteWhere { DeviceTokenTable.userId eq userId }
    }
}
