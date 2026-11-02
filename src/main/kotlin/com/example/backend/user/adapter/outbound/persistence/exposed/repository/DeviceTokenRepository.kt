package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.DeviceTokenTable
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.upsert
import org.springframework.stereotype.Repository

@Repository
class DeviceTokenRepository {
    /**
     * 사용자 기기 단위 업서트. 같은 기기면 토큰과 헤더 값을 새 요청으로 덮어쓴다(토큰 갱신).
     * 같은 토큰을 가진 다른 사용자의 행은 지운다. 토큰은 실제 기기만 가진 값이라 계정 전환의 소유 증명이 되고,
     * 이전 계정의 알림이 지금 기기 사용자에게 가지 않게 한다. 기기 ID 만으로는 남의 행을 건드릴 수 없다.
     */
    fun upsert(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    ) {
        DeviceTokenTable.deleteWhere {
            (DeviceTokenTable.fcmToken eq command.fcmToken) and
                (DeviceTokenTable.userId neq userId)
        }
        DeviceTokenTable.upsert(
            DeviceTokenTable.userId,
            DeviceTokenTable.deviceId,
            onUpdate = {
                it[DeviceTokenTable.fcmToken] = insertValue(DeviceTokenTable.fcmToken)
                it[DeviceTokenTable.platform] = insertValue(DeviceTokenTable.platform)
                it[DeviceTokenTable.appBuild] = insertValue(DeviceTokenTable.appBuild)
                it[DeviceTokenTable.lastSeenAt] = CurrentTimestamp
            },
        ) {
            it[DeviceTokenTable.userId] = userId
            it[DeviceTokenTable.deviceId] = command.deviceId
            it[DeviceTokenTable.fcmToken] = command.fcmToken
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
