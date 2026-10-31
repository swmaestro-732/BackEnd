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
    /** 기기당 한 행. 같은 기기면 토큰, 소유자, 헤더 값을 새 요청으로 덮어쓴다(토큰 갱신, 계정 전환). */
    fun upsert(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    ) {
        DeviceTokenTable.upsert(
            DeviceTokenTable.deviceId,
            onUpdate = {
                it[DeviceTokenTable.userId] = insertValue(DeviceTokenTable.userId)
                it[DeviceTokenTable.token] = insertValue(DeviceTokenTable.token)
                it[DeviceTokenTable.platform] = insertValue(DeviceTokenTable.platform)
                it[DeviceTokenTable.appBuild] = insertValue(DeviceTokenTable.appBuild)
                it[DeviceTokenTable.lastSeenAt] = CurrentTimestamp
            },
        ) {
            it[DeviceTokenTable.userId] = userId
            it[DeviceTokenTable.deviceId] = command.deviceId
            it[DeviceTokenTable.token] = command.token
            it[DeviceTokenTable.platform] = command.platform
            it[DeviceTokenTable.appBuild] = command.appBuild
        }
    }

    fun deleteByUserAndDevice(
        userId: Long,
        deviceId: String,
    ) {
        DeviceTokenTable.deleteWhere { (DeviceTokenTable.userId eq userId) and (DeviceTokenTable.deviceId eq deviceId) }
    }

    fun deleteAllByUser(userId: Long) {
        DeviceTokenTable.deleteWhere { DeviceTokenTable.userId eq userId }
    }
}
