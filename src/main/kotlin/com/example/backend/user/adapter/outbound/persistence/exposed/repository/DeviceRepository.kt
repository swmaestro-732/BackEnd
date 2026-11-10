package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.DeviceTable
import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.user.application.port.outbound.LockedDevice
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.case
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.select
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
                val sameToken = DeviceTable.fcmToken eq insertValue(DeviceTable.fcmToken)
                // 광고 동의는 그 토큰(그 설치)에 대한 것이다. 토큰이 바뀌면 동의를 지워, 기기 ID 로 토큰을 바꿔 심은 동의가
                // 실제 설치로 넘어가지 않게 한다. 단 이미 이 계정에 연결된 기기를 같은 계정이 로그인 상태로 갱신하면
                // 본인 기기의 토큰 교체(FCM 토큰 갱신, 무효 토큰 정리 후 재등록)로 보고 동의를 유지한다.
                val keepConsent = if (userId != null) sameToken or (DeviceTable.userId eq userId) else sameToken
                it[DeviceTable.marketingAgreedAt] =
                    case().When(keepConsent, DeviceTable.marketingAgreedAt).Else(Op.nullOp())
                it[DeviceTable.nightAgreedAt] = case().When(keepConsent, DeviceTable.nightAgreedAt).Else(Op.nullOp())
                it[DeviceTable.fcmToken] = insertValue(DeviceTable.fcmToken)
                // 비로그인 경로는 인증이 없어 기기 ID 만으로 계정에 연결된 행의 토큰을 바꿀 수 있다.
                // 토큰이 바뀌면 연결을 끊어, 남의 기기 ID 로 그 계정의 활동 알림을 가로채지 못하게 한다.
                it[DeviceTable.userId] =
                    if (userId != null) {
                        insertValue(DeviceTable.userId)
                    } else {
                        case().When(sameToken, DeviceTable.userId).Else(Op.nullOp())
                    }
                it[DeviceTable.lastSeenAt] = CurrentTimestamp
            },
        ) {
            it[deviceId] = command.deviceId
            it[DeviceTable.userId] = userId
            it[fcmToken] = command.fcmToken
        }
    }

    fun lockConsent(deviceId: String): LockedDevice {
        DeviceTable.insertIgnore { it[DeviceTable.deviceId] = deviceId }
        return DeviceTable
            .selectAll()
            .where { DeviceTable.deviceId eq deviceId }
            .forUpdate()
            .single()
            .let { LockedDevice(it[DeviceTable.fcmToken], toConsent(it), it[DeviceTable.userId] != null) }
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

    fun clearTokens(fcmTokens: Collection<String>) {
        DeviceTable.update({ DeviceTable.fcmToken inList fcmTokens }) { it[fcmToken] = null }
    }

    fun findMarketingTokens(night: Boolean): List<String> {
        val agreed = DeviceTable.fcmToken.isNotNull() and DeviceTable.marketingAgreedAt.isNotNull()
        // fcm_token 은 UNIQUE 라 중복 제거(DISTINCT)가 필요 없다.
        return DeviceTable
            .select(DeviceTable.fcmToken)
            .where { if (night) agreed and DeviceTable.nightAgreedAt.isNotNull() else agreed }
            .map { checkNotNull(it[DeviceTable.fcmToken]) }
    }

    private fun toConsent(row: org.jetbrains.exposed.v1.core.ResultRow): DeviceConsent =
        DeviceConsent(
            row[DeviceTable.marketingAgreedAt]?.toJavaInstant(),
            row[DeviceTable.nightAgreedAt]?.toJavaInstant(),
        )
}
