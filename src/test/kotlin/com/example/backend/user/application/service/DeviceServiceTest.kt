package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.user.application.port.inbound.dto.UpdateDeviceCommand
import com.example.backend.user.application.port.outbound.DevicePersistencePort
import com.example.backend.user.application.port.outbound.LockedDevice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DeviceServiceTest {
    private val now = Instant.parse("2026-06-01T00:00:00Z")
    private val past = Instant.parse("2025-01-01T00:00:00Z")

    private val devicePersistencePort = FakeDevicePersistencePort()
    private val service = DeviceService(devicePersistencePort, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `광고 동의 true는 현재 시각으로 동의하고 야간은 건드리지 않는다`() {
        val result = service.update(command(marketing = true))

        assertEquals(DeviceConsent(now, null), result)
        assertEquals(result, devicePersistencePort.consent)
    }

    @Test
    fun `이미 동의한 상태에서 다시 동의해도 처음 동의 시각을 유지한다`() {
        devicePersistencePort.consent = DeviceConsent(past, past)

        val result = service.update(command(marketing = true, night = true))

        assertEquals(DeviceConsent(past, past), result)
    }

    @Test
    fun `보내지 않은 필드는 기존 동의를 바꾸지 않는다`() {
        devicePersistencePort.consent = DeviceConsent(past, past)

        val result = service.update(command(night = true))

        assertEquals(DeviceConsent(past, past), result)
    }

    @Test
    fun `광고 동의를 철회하면 야간 동의도 함께 철회한다`() {
        devicePersistencePort.consent = DeviceConsent(past, past)

        val result = service.update(command(marketing = false))

        assertEquals(DeviceConsent.NONE, result)
        assertEquals(DeviceConsent.NONE, devicePersistencePort.consent)
    }

    @Test
    fun `야간 동의 false는 야간만 철회한다`() {
        devicePersistencePort.consent = DeviceConsent(past, past)

        val result = service.update(command(night = false))

        assertEquals(DeviceConsent(past, null), result)
    }

    @Test
    fun `광고 동의 없이 야간 동의 true면 INVALID_INPUT이고 저장하지 않는다`() {
        val exception = assertThrows<BusinessException> { service.update(command(night = true)) }

        assertEquals(CommonErrorCode.INVALID_INPUT, exception.errorCode)
        assertEquals(0, devicePersistencePort.updateCount)
    }

    @Test
    fun `같은 요청에서 광고 철회와 야간 동의를 함께 보내면 INVALID_INPUT이다`() {
        devicePersistencePort.consent = DeviceConsent(past, null)

        assertThrows<BusinessException> { service.update(command(marketing = false, night = true)) }
        assertEquals(DeviceConsent(past, null), devicePersistencePort.consent)
    }

    @Test
    fun `같은 토큰만 다시 보내면 토큰만 저장하고 현재 동의를 돌려준다`() {
        devicePersistencePort.storedToken = "tok"
        devicePersistencePort.consent = DeviceConsent(past, null)

        val result = service.update(command(token = "tok"))

        assertEquals(DeviceConsent(past, null), result)
        assertEquals(RegisterDeviceTokenCommand(DEVICE, "tok") to null, devicePersistencePort.saved.single())
    }

    @Test
    fun `기기가 없으면 조회는 미동의를 돌려준다`() {
        devicePersistencePort.exists = false

        assertEquals(DeviceConsent.NONE, service.getConsent(DEVICE))
        assertNull(devicePersistencePort.saved.firstOrNull())
    }

    @Test
    fun `토큰 없는 동의만 보내면 계정 연결은 지정하지 않는다`() {
        service.update(command(marketing = true))

        assertTrue(devicePersistencePort.saved.isEmpty())
    }

    private fun command(
        token: String? = null,
        marketing: Boolean? = null,
        night: Boolean? = null,
    ) = UpdateDeviceCommand(DEVICE, token, marketing, night)

    private class FakeDevicePersistencePort : DevicePersistencePort {
        var consent = DeviceConsent.NONE
        var storedToken: String? = null
        var exists = true
        var updateCount = 0
        val saved = mutableListOf<Pair<RegisterDeviceTokenCommand, Long?>>()

        override fun saveToken(
            command: RegisterDeviceTokenCommand,
            userId: Long?,
        ) {
            saved += command to userId
        }

        override fun lockConsent(deviceId: String): LockedDevice = LockedDevice(storedToken, consent)

        override fun updateConsent(
            deviceId: String,
            marketingAgreedAt: Instant?,
            nightAgreedAt: Instant?,
        ) {
            updateCount++
            consent = DeviceConsent(marketingAgreedAt, nightAgreedAt)
        }

        override fun findConsent(deviceId: String): DeviceConsent? = if (exists) consent else null

        override fun unlink(
            userId: Long,
            deviceId: String,
        ) = Unit

        override fun unlinkAllByUser(userId: Long) = Unit

        override fun recordActivity(
            deviceId: String,
            date: java.time.LocalDate,
            userId: Long?,
            platform: String?,
        ) = Unit

        override fun clearTokens(fcmTokens: Collection<String>) = Unit

        override fun findMarketingTokens(night: Boolean): List<String> = emptyList()
    }

    private companion object {
        const val DEVICE = "fid-unit"
    }
}
