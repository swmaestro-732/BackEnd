package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.DeviceTable
import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.springframework.stereotype.Repository
import java.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Repository
class DeviceRepository {
    fun saveToken(
        command: RegisterDeviceTokenCommand,
        userId: Long?,
    ) {
        // 토큰은 실제 설치만 가진 값이라 소유 증명이 된다. 같은 토큰이 다른 기기 ID 에 남아 있으면 그 행의 토큰과 연결을 비운다.
        DeviceTable.update({
            (DeviceTable.fcmToken eq command.fcmToken) and (DeviceTable.deviceId neq command.deviceId)
        }) {
            it[fcmToken] = null
            it[DeviceTable.userId] = null
        }
        DeviceTable.upsert(
            DeviceTable.deviceId,
            onUpdate = {
                it[DeviceTable.fcmToken] = insertValue(DeviceTable.fcmToken)
                it[DeviceTable.platform] = insertValue(DeviceTable.platform)
                it[DeviceTable.appBuild] = insertValue(DeviceTable.appBuild)
                if (userId != null) it[DeviceTable.userId] = insertValue(DeviceTable.userId)
                it[DeviceTable.lastSeenAt] = CurrentTimestamp
            },
        ) {
            it[deviceId] = command.deviceId
            it[DeviceTable.userId] = userId
            it[fcmToken] = command.fcmToken
            it[platform] = command.platform
            it[appBuild] = command.appBuild
        }
    }

    fun lockConsent(deviceId: String): DeviceConsent {
        DeviceTable.insertIgnore { it[DeviceTable.deviceId] = deviceId }
        return DeviceTable
            .selectAll()
            .where { DeviceTable.deviceId eq deviceId }
            .forUpdate()
            .single()
            .let(::toConsent)
    }

    fun updateConsent(
        deviceId: String,
        marketingAgreedAt: Instant?,
        nightAgreedAt: Instant?,
    ) {
        DeviceTable.update({ DeviceTable.deviceId eq deviceId }) {
            it[DeviceTable.marketingAgreedAt] = marketingAgreedAt?.toKotlinInstant()
            it[DeviceTable.nightAgreedAt] = nightAgreedAt?.toKotlinInstant()
            it[lastSeenAt] = CurrentTimestamp
        }
    }

    fun findConsent(deviceId: String): DeviceConsent? =
        DeviceTable
            .selectAll()
            .where { DeviceTable.deviceId eq deviceId }
            .singleOrNull()
            ?.let(::toConsent)

    fun unlink(
        userId: Long,
        deviceId: String,
    ) {
        DeviceTable.update({ (DeviceTable.userId eq userId) and (DeviceTable.deviceId eq deviceId) }) {
            it[DeviceTable.userId] = null
        }
    }

    fun unlinkAllByUser(userId: Long) {
        DeviceTable.update({ DeviceTable.userId eq userId }) { it[DeviceTable.userId] = null }
    }

    private fun toConsent(row: org.jetbrains.exposed.v1.core.ResultRow): DeviceConsent =
        DeviceConsent(
            row[DeviceTable.marketingAgreedAt]?.toJavaInstant(),
            row[DeviceTable.nightAgreedAt]?.toJavaInstant(),
        )
}
