package com.example.backend.user.application.service

import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.user.application.port.outbound.DevicePersistencePort
import com.example.backend.user.application.port.outbound.LockedDevice
import com.example.backend.user.application.port.outbound.UserPersistencePort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.core.task.TaskExecutor
import org.springframework.core.task.TaskRejectedException
import org.springframework.transaction.support.TransactionOperations
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DeviceActivityServiceTest {
    private val devicePersistencePort = FakeDevicePersistencePort()
    private val sync = TaskExecutor { it.run() }

    // 1L, 3L 은 활성 사용자, 그 밖의 id 는 탈퇴했거나 없는 사용자다(목의 기본값은 빈 집합).
    private val userPersistencePort =
        mock(UserPersistencePort::class.java).also {
            `when`(it.lockActiveShared(listOf(1L))).thenReturn(setOf(1L))
            `when`(it.lockActiveShared(listOf(3L))).thenReturn(setOf(3L))
        }

    private fun service(executor: TaskExecutor) =
        DeviceActivityService(
            devicePersistencePort,
            userPersistencePort,
            TransactionOperations.withoutTransaction(),
            // 반복 기록 중 KST 자정을 넘으면 새 날로 보고 한 번 더 쓰므로 시계를 고정한다.
            Clock.fixed(Instant.parse("2026-11-13T03:00:00Z"), ZoneOffset.UTC),
            executor,
        )

    @Test
    fun `같은 날 같은 기기는 한 번만 쓰고 로그인하면 한 번 더 쓴다`() {
        val service = service(sync)

        repeat(3) { service.record("device-a", null, "android") }
        repeat(3) { service.record("device-a", 1L, "android") }

        assertEquals(listOf("device-a" to null, "device-a" to 1L), devicePersistencePort.recorded)
    }

    @Test
    fun `같은 날 같은 기기에 다른 계정이 로그인하면 한 번 더 쓴다`() {
        val service = service(sync)

        service.record("device-a", 1L, "android")
        repeat(2) { service.record("device-a", 3L, "android") }

        assertEquals(listOf("device-a" to 1L, "device-a" to 3L), devicePersistencePort.recorded)
    }

    @Test
    fun `플랫폼 없이 먼저 기록된 기기는 첫 유효 플랫폼이 오면 한 번 더 쓴다`() {
        val service = service(sync)

        service.record("device-a", null, null)
        repeat(2) { service.record("device-a", null, "android") }

        assertEquals(listOf("device-a" to null, "device-a" to null), devicePersistencePort.recorded)
    }

    @Test
    fun `큐가 차서 거절되면 다음 요청에 같은 기기를 다시 기록하려 한다`() {
        var calls = 0
        val rejecting =
            TaskExecutor {
                calls++
                throw TaskRejectedException("full")
            }
        val service = service(rejecting)

        service.record("device-a", null, null)
        service.record("device-a", null, null)

        assertEquals(2, calls)
    }

    @Test
    fun `쓰기가 실패하면 다음 요청에 같은 기기를 다시 기록하려 한다`() {
        devicePersistencePort.failing = true
        val service = service(sync)

        service.record("device-a", null, null)
        service.record("device-a", null, null)

        assertEquals(2, devicePersistencePort.recorded.size)
    }

    @Test
    fun `탈퇴한 사용자의 늦은 활동 기록은 user_id 없이 쓴다`() {
        service(sync).record("device-a", 2L, "android")

        assertEquals(listOf("device-a" to null), devicePersistencePort.recorded)
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

        override fun clearTokens(fcmTokens: Collection<String>) = Unit

        override fun findMarketingTokens(night: Boolean): List<String> = emptyList()
    }
}
