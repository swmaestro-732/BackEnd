package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.adapter.outbound.persistence.exposed.DeviceTokenTable
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.user.application.port.outbound.DeviceTokenPersistencePort
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.time.Instant

class DeviceTokenPersistenceTest
    @Autowired
    constructor(
        private val port: DeviceTokenPersistencePort,
    ) : IntegrationTestBase() {
        @Test
        fun `upsert는 사용자와 헤더 값을 저장하고 생성 시간을 채운다`() {
            transaction {
                insertUsers()
                val token = "device-persistence-insert"
                port.upsert(ME, command(token, "ANDROID", 12))

                val row = row(token)
                assertEquals(ME, row[DeviceTokenTable.userId])
                assertEquals(token, row[DeviceTokenTable.token])
                assertEquals("ANDROID", row[DeviceTokenTable.platform])
                assertEquals(12, row[DeviceTokenTable.appBuild])
                assertEquals(row[DeviceTokenTable.createdAt], row[DeviceTokenTable.lastSeenAt])
                assertTrue(row[DeviceTokenTable.createdAt] > OLD)
                rollback()
            }
        }

        @Test
        fun `재등록은 같은 행의 헤더와 마지막 접속만 갱신하고 생성 시각은 보존한다`() {
            transaction {
                insertUsers()
                val token = "device-persistence-refresh"
                port.upsert(ME, command(token, "ANDROID", 1))
                val id = row(token)[DeviceTokenTable.id]
                DeviceTokenTable.update({ DeviceTokenTable.token eq token }) {
                    it[createdAt] = OLD
                    it[lastSeenAt] = OLD
                }

                port.upsert(ME, command(token, "IOS", 2))

                val row = row(token)
                assertEquals(id, row[DeviceTokenTable.id])
                assertEquals(ME, row[DeviceTokenTable.userId])
                assertEquals("IOS", row[DeviceTokenTable.platform])
                assertEquals(2, row[DeviceTokenTable.appBuild])
                assertEquals(OLD, row[DeviceTokenTable.createdAt])
                assertTrue(row[DeviceTokenTable.lastSeenAt] > OLD)
                assertEquals(1L, DeviceTokenTable.selectAll().where { DeviceTokenTable.token eq token }.count())
                rollback()
            }
        }

        @Test
        fun `다른 사용자의 upsert는 소유권을 이전하고 누락된 헤더 값을 지운다`() {
            transaction {
                insertUsers()
                val token = "device-persistence-transfer"
                port.upsert(ME, command(token, "ANDROID", 1))
                val original = row(token)

                port.upsert(OTHER, command(token, null, null))

                val row = row(token)
                assertEquals(original[DeviceTokenTable.id], row[DeviceTokenTable.id])
                assertEquals(original[DeviceTokenTable.createdAt], row[DeviceTokenTable.createdAt])
                assertEquals(OTHER, row[DeviceTokenTable.userId])
                assertNull(row[DeviceTokenTable.platform])
                assertNull(row[DeviceTokenTable.appBuild])
                assertEquals(1L, DeviceTokenTable.selectAll().where { DeviceTokenTable.token eq token }.count())
                rollback()
            }
        }

        @Test
        fun `토큰의 SQL 특수문자는 값으로 저장되고 재등록된다`() {
            transaction {
                insertUsers()
                val token = "device-token'; DELETE FROM device_tokens; --"
                port.upsert(ME, command(token, null, null))
                port.upsert(OTHER, command(token, "IOS", 3))

                val row = row(token)
                assertEquals(token, row[DeviceTokenTable.token])
                assertEquals(OTHER, row[DeviceTokenTable.userId])
                assertEquals("IOS", row[DeviceTokenTable.platform])
                assertEquals(3, row[DeviceTokenTable.appBuild])
                rollback()
            }
        }

        @Test
        fun `deleteByUserAndDevice는 소유자가 맞는 행만 삭제하고 반복해도 성공한다`() {
            transaction {
                insertUsers()
                val token = "device-persistence-delete"
                port.upsert(ME, command(token, null, null))

                port.deleteByUserAndDevice(OTHER, token)
                assertEquals(ME, row(token)[DeviceTokenTable.userId])
                port.deleteByUserAndDevice(ME, "unknown-device-token")
                assertEquals(ME, row(token)[DeviceTokenTable.userId])

                port.deleteByUserAndDevice(ME, token)
                port.deleteByUserAndDevice(ME, token)

                assertEquals(0L, DeviceTokenTable.selectAll().where { DeviceTokenTable.token eq token }.count())
                rollback()
            }
        }

        @Test
        fun `같은 기기에서 토큰이 바뀌면 행 하나에서 토큰만 교체한다`() {
            transaction {
                insertUsers()
                port.upsert(ME, command("device-persistence-old", "ANDROID", 1, deviceId = "fid-rotate"))
                port.upsert(ME, command("device-persistence-new", "ANDROID", 1, deviceId = "fid-rotate"))

                val rows = DeviceTokenTable.selectAll().where { DeviceTokenTable.deviceId eq "fid-rotate" }.toList()
                assertEquals(1, rows.size)
                assertEquals("device-persistence-new", rows.single()[DeviceTokenTable.token])
                rollback()
            }
        }

        @Test
        fun `deleteAllByUser는 사용자의 모든 토큰을 삭제하고 다른 사용자의 토큰은 보존한다`() {
            transaction {
                insertUsers()
                port.upsert(ME, command("device-persistence-a", "ANDROID", 1))
                port.upsert(ME, command("device-persistence-b", "IOS", 2))
                port.upsert(OTHER, command("device-persistence-other", null, null))

                port.deleteAllByUser(ME)
                port.deleteAllByUser(ME)

                assertEquals(0L, DeviceTokenTable.selectAll().where { DeviceTokenTable.userId eq ME }.count())
                assertEquals(
                    listOf("device-persistence-other"),
                    DeviceTokenTable
                        .selectAll()
                        .where { DeviceTokenTable.userId eq OTHER }
                        .map { it[DeviceTokenTable.token] },
                )
                rollback()
            }
        }

        // device_tokens.user_id 는 users(id) FK 라 소유자 행이 먼저 있어야 한다(테스트 트랜잭션과 함께 롤백).
        private fun insertUsers() {
            TransactionManager.current().exec(
                "INSERT INTO users (id, nickname) VALUES ($ME, '기기주인'), ($OTHER, '다른사용자')",
            )
        }

        /** 기본값으로 기기 ID 를 토큰과 같게 둬 토큰으로 행을 찾는다. */
        private fun command(
            token: String,
            platform: String?,
            appBuild: Int?,
            deviceId: String = token,
        ) = RegisterDeviceTokenCommand(deviceId, token, platform, appBuild)

        private fun row(token: String) = DeviceTokenTable.selectAll().where { DeviceTokenTable.token eq token }.single()

        private companion object {
            const val ME = 730101L
            const val OTHER = 730102L
            val OLD = Instant.parse("2000-01-01T00:00:00Z")
        }
    }
