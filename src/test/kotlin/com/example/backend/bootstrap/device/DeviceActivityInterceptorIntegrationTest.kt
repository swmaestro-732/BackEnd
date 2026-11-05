package com.example.backend.bootstrap.device

import com.example.backend.bootstrap.config.AsyncConfig
import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.adapter.outbound.persistence.exposed.DeviceActivityTable
import com.example.backend.user.application.port.outbound.DevicePersistencePort
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@AutoConfigureMockMvc
class DeviceActivityInterceptorIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
        private val devicePersistencePort: DevicePersistencePort,
        @Qualifier(AsyncConfig.DEVICE_ACTIVITY_EXECUTOR) private val deviceActivityExecutor: ThreadPoolTaskExecutor,
    ) : IntegrationTestBase() {
        @AfterEach
        fun cleanUp() {
            transaction { DeviceActivityTable.deleteAll() }
        }

        @Test
        fun `같은 기기가 하루에 여러 번 요청해도 활동은 한 행만 남는다`() {
            // 기록기의 메모리 중복 필터는 컨텍스트를 공유하는 다른 테스트와 겹치지 않게 고유 ID 를 쓴다.
            val deviceA = UUID.randomUUID().toString()
            val deviceB = UUID.randomUUID().toString()
            repeat(3) {
                mockMvc.perform(get("/api/v1/courses/999999").header(HEADER, deviceA)).andExpect(status().isNotFound)
            }
            mockMvc.perform(get("/api/v1/courses/999999").header(HEADER, deviceB))

            // 쓰기는 비동기라 실행기 큐가 빌 때까지 기다린다.
            deviceActivityExecutor.awaitIdle()
            val rows =
                transaction {
                    DeviceActivityTable
                        .selectAll()
                        .where { DeviceActivityTable.deviceId inList listOf(deviceA, deviceB) }
                        .map { it[DeviceActivityTable.deviceId] }
                }
            assertThat(rows).containsExactlyInAnyOrder(deviceA, deviceB)
        }

        @Test
        fun `헤더가 없거나 너무 길면 기록하지 않고 요청은 그대로 처리한다`() {
            val before = transaction { DeviceActivityTable.selectAll().count() }
            mockMvc.perform(get("/api/v1/courses/999999")).andExpect(status().isNotFound)
            mockMvc
                .perform(get("/api/v1/courses/999999").header(HEADER, "x".repeat(129)))
                .andExpect(status().isNotFound)

            deviceActivityExecutor.awaitIdle()
            assertThat(transaction { DeviceActivityTable.selectAll().count() }).isEqualTo(before)
        }

        @Test
        fun `비로그인으로 먼저 기록된 기기가 그날 로그인하면 user_id 를 채우고 탈퇴하면 지운다`() {
            val device = UUID.randomUUID().toString()
            mockMvc.perform(
                get("/api/v1/courses/999999")
                    .header(HEADER, device)
                    .header("X-App-Platform", "Android")
                    .header("X-App-Build", "999"),
            )
            deviceActivityExecutor.awaitIdle()
            mockMvc.perform(
                get("/api/v1/courses/999999")
                    .header(HEADER, device)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(USER_ID)}"),
            )
            deviceActivityExecutor.awaitIdle()

            assertThat(activityOf(device)).containsExactly(USER_ID to "android")

            transaction { devicePersistencePort.unlinkAllByUser(USER_ID) }
            assertThat(activityOf(device)).containsExactly(null to "android")
        }

        private fun activityOf(device: String): List<Pair<Long?, String?>> =
            transaction {
                DeviceActivityTable
                    .selectAll()
                    .where { DeviceActivityTable.deviceId eq device }
                    .map { it[DeviceActivityTable.userId] to it[DeviceActivityTable.platform] }
            }

        private fun ThreadPoolTaskExecutor.awaitIdle() {
            val deadline = System.nanoTime() + 5_000_000_000
            while ((threadPoolExecutor.queue.isNotEmpty() || activeCount > 0) && System.nanoTime() < deadline) {
                Thread.sleep(10)
            }
        }

        private companion object {
            const val HEADER = CurrentDeviceIdArgumentResolver.DEVICE_ID_HEADER
            const val USER_ID = 987_654_321L
        }
    }
