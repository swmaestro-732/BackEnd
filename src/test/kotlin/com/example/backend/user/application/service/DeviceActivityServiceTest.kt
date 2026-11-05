package com.example.backend.user.application.service

import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.user.application.port.outbound.DevicePersistencePort
import com.example.backend.user.application.port.outbound.LockedDevice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class DeviceActivityServiceTest {
    private val devicePersistencePort = FakeDevicePersistencePort()
    private val sync = TaskExecutor { it.run() }

    @Test
    fun `같은 날 같은 기기는 한 번만 쓰고 로그인하면 한 번 더 쓴다`() {
        val service = DeviceActivityService(devicePersistencePort, Clock.systemUTC(), sync)

        repeat(3) { service.record("device-a", null, "android") }
        repeat(3) { service.record("device-a", 1L, "android") }

        assertEquals(listOf("device-a" to null, "device-a" to 1L), devicePersistencePort.recorded)
    }

    @Test
    fun `큐가 차서 거절되면 다음 요청에 같은 기기를 다시 기록하려 한다`() {
        var calls = 0
        val rejecting =
            TaskExecutor {
                calls++
                throw TaskRejectedException("full")
            }
        val service = DeviceActivityService(devicePersistencePort, Clock.systemUTC(), rejecting)

        service.record("device-a", null, null)
        service.record("device-a", null, null)

        assertEquals(2, calls)
    }

    @Test
    fun `쓰기가 실패하면 다음 요청에 같은 기기를 다시 기록하려 한다`() {
        devicePersistencePort.failing = true
        val service = DeviceActivityService(devicePersistencePort, Clock.systemUTC(), sync)

        service.record("device-a", null, null)
        service.record("device-a", null, null)

        assertEquals(2, devicePersistencePort.recorded.size)
    }

    private class FakeDevicePersistencePort : DevicePersistencePort {
        val recorded = mutableListOf<Pair<String, Long?>>()
        var failing = false

        override fun recordActivity(
            deviceId: String,
            date: LocalDate,
            userId: Long?,
            platform: String?,
        ) {
            recorded += deviceId to userId
            if (failing) throw IllegalStateException("db down")
        }

        override fun saveToken(
            command: RegisterDeviceTokenCommand,
            userId: Long?,
        ) = Unit

        override fun lockConsent(deviceId: String) = LockedDevice(null, DeviceConsent.NONE)

        override fun updateConsent(
            deviceId: String,
            marketingAgreedAt: Instant?,
            nightAgreedAt: Instant?,
        ) = Unit

        override fun findConsent(deviceId: String): DeviceConsent? = null

        override fun unlink(
            userId: Long,
            deviceId: String,
        ) = Unit

        override fun unlinkAllByUser(userId: Long) = Unit
    }
}
