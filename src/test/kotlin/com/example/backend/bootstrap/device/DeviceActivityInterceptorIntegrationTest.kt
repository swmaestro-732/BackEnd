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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import java.util.concurrent.TimeUnit

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
            transaction {
                DeviceActivityTable.deleteAll()
                exec("DELETE FROM users WHERE id = $USER_ID")
            }
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
        fun `백그라운드 기기 API 요청은 활동으로 세지 않는다`() {
            val device = UUID.randomUUID().toString()
            mockMvc.perform(get("/api/v1/devices").header(HEADER, device))
            // /api/v1/my/** 는 인증 필수라 토큰이 없으면 보안 필터에서 막혀 인터셉터까지 오지 않는다.
            mockMvc
                .perform(
                    delete("/api/v1/my/devices")
                        .header(HEADER, device)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(USER_ID)}"),
                ).andExpect(status().isOk)

            deviceActivityExecutor.awaitIdle()
            assertThat(activityOf(device)).isEmpty()
        }

        @Test
        fun `비로그인으로 먼저 기록된 기기가 그날 로그인하면 user_id 를 채우고 탈퇴하면 지운다`() {
            val device = UUID.randomUUID().toString()
            // 활동에는 활성 사용자만 user_id 로 남으므로 사용자 행이 있어야 한다.
            transaction { exec("INSERT INTO users (id, nickname) VALUES ($USER_ID, '활동사용자') ON CONFLICT DO NOTHING") }
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

        @Test
        fun `탈퇴한 사용자의 남은 토큰으로 요청하면 활동은 남기되 user_id 는 비운다`() {
            val device = UUID.randomUUID().toString()
            transaction {
                exec(
                    "INSERT INTO users (id, nickname, deleted_at) VALUES ($USER_ID, '탈퇴사용자', now()) ON CONFLICT DO NOTHING",
                )
            }
            mockMvc.perform(
                get("/api/v1/courses/999999")
                    .header(HEADER, device)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${jwtTokenProvider.issueAccessToken(USER_ID)}"),
            )
            deviceActivityExecutor.awaitIdle()

            assertThat(activityOf(device)).containsExactly(null to null)
        }

        private fun activityOf(device: String): List<Pair<Long?, String?>> =
            transaction {
                DeviceActivityTable
                    .selectAll()
                    .where { DeviceActivityTable.deviceId eq device }
                    .map { it[DeviceActivityTable.userId] to it[DeviceActivityTable.platform] }
            }

        // 실행기 스레드가 하나라 빈 작업이 끝나면 앞서 제출된 쓰기도 끝난 것이다.
        // 큐와 활성 스레드 수를 보는 방식은 워커가 작업을 꺼낸 직후 둘 다 0 으로 보여 쓰기 전에 통과할 수 있다.
        private fun ThreadPoolTaskExecutor.awaitIdle() {
            submit {}.get(5, TimeUnit.SECONDS)
        }

        private companion object {
            const val HEADER = CurrentDeviceIdArgumentResolver.DEVICE_ID_HEADER
            const val USER_ID = 987_654_321L
        }
    }
