package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.adapter.outbound.persistence.exposed.DeviceActivityTable
import com.example.backend.user.adapter.outbound.persistence.exposed.DeviceTable
import com.example.backend.user.adapter.outbound.persistence.exposed.UserTable
import com.example.backend.user.application.port.inbound.dto.DeviceConsent
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import com.example.backend.user.application.port.outbound.DevicePersistencePort
import com.example.backend.user.application.port.outbound.LockedDevice
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteAll
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

class DevicePersistenceTest
    @Autowired
    constructor(
        private val devicePersistencePort: DevicePersistencePort,
    ) : IntegrationTestBase() {
        @Test
        fun `saveToken은 기기 행을 만들고 사용자와 토큰을 저장한다`() {
            transaction {
                insertUsers()
                devicePersistencePort.saveToken(command("fid-insert", "token-insert"), ME)

                val row = row("fid-insert")
                assertEquals(ME, row[DeviceTable.userId])
                assertEquals("token-insert", row[DeviceTable.fcmToken])
                assertNull(row[DeviceTable.marketingAgreedAt])
                assertEquals(row[DeviceTable.createdAt], row[DeviceTable.lastSeenAt])
                rollback()
            }
        }

        @Test
        fun `재등록은 같은 행의 토큰과 마지막 접속만 갱신하고 생성 시각은 보존한다`() {
            transaction {
                insertUsers()
                devicePersistencePort.saveToken(command("fid-refresh", "token-old"), ME)
                DeviceTable.update({ DeviceTable.deviceId eq "fid-refresh" }) {
                    it[createdAt] = OLD
                    it[lastSeenAt] = OLD
                }

                devicePersistencePort.saveToken(command("fid-refresh", "token-new"), ME)

                val row = row("fid-refresh")
                assertEquals("token-new", row[DeviceTable.fcmToken])
                assertEquals(OLD, row[DeviceTable.createdAt])
                assertTrue(row[DeviceTable.lastSeenAt] > OLD)
                assertEquals(1L, DeviceTable.selectAll().where { DeviceTable.deviceId eq "fid-refresh" }.count())
                rollback()
            }
        }

        @Test
        fun `userId가 null이면 같은 토큰일 때만 연결을 유지하고 토큰이 바뀌면 연결을 끊는다`() {
            transaction {
                insertUsers()
                devicePersistencePort.saveToken(command("fid-keep", "token-keep"), ME)
                devicePersistencePort.saveToken(command("fid-swap", "token-swap"), ME)

                devicePersistencePort.saveToken(command("fid-keep", "token-keep"), null)
                devicePersistencePort.saveToken(command("fid-swap", "token-attacker"), null)
                devicePersistencePort.saveToken(command("fid-guest", "token-guest"), null)

                assertEquals(ME, row("fid-keep")[DeviceTable.userId])
                assertNull(row("fid-swap")[DeviceTable.userId])
                assertEquals("token-attacker", row("fid-swap")[DeviceTable.fcmToken])
                assertNull(row("fid-guest")[DeviceTable.userId])
                rollback()
            }
        }

        @Test
        fun `userId가 있으면 같은 기기의 이전 연결을 덮어쓴다`() {
            transaction {
                insertUsers()
                devicePersistencePort.saveToken(command("fid-takeover", "token-takeover"), ME)

                devicePersistencePort.saveToken(command("fid-takeover", "token-takeover"), OTHER)

                assertEquals(OTHER, row("fid-takeover")[DeviceTable.userId])
                assertEquals(1L, DeviceTable.selectAll().where { DeviceTable.deviceId eq "fid-takeover" }.count())
                rollback()
            }
        }

        @Test
        fun `같은 토큰을 다른 기기가 등록하면 이전 기기 행의 토큰과 연결을 비우고 행은 남긴다`() {
            transaction {
                insertUsers()
                devicePersistencePort.saveToken(command("fid-first", "token-shared"), ME)

                devicePersistencePort.saveToken(command("fid-second", "token-shared"), OTHER)

                val first = row("fid-first")
                assertNull(first[DeviceTable.fcmToken])
                assertNull(first[DeviceTable.userId])
                assertEquals("token-shared", row("fid-second")[DeviceTable.fcmToken])
                assertEquals(OTHER, row("fid-second")[DeviceTable.userId])
                rollback()
            }
        }

        @Test
        fun `토큰의 SQL 특수문자는 값으로 저장된다`() {
            transaction {
                insertUsers()
                val token = "device-token'; DELETE FROM devices; --"
                devicePersistencePort.saveToken(command("fid-sql", token), ME)
                devicePersistencePort.saveToken(command("fid-sql", token), OTHER)

                val row = row("fid-sql")
                assertEquals(token, row[DeviceTable.fcmToken])
                assertEquals(OTHER, row[DeviceTable.userId])
                rollback()
            }
        }

        @Test
        fun `unlink는 소유자가 맞는 연결만 끊고 토큰과 행은 남기며 반복해도 성공한다`() {
            transaction {
                insertUsers()
                devicePersistencePort.saveToken(command("fid-unlink", "token-unlink"), ME)

                devicePersistencePort.unlink(OTHER, "fid-unlink")
                devicePersistencePort.unlink(ME, "fid-unknown")
                assertEquals(ME, row("fid-unlink")[DeviceTable.userId])

                devicePersistencePort.unlink(ME, "fid-unlink")
                devicePersistencePort.unlink(ME, "fid-unlink")

                val row = row("fid-unlink")
                assertNull(row[DeviceTable.userId])
                assertEquals("token-unlink", row[DeviceTable.fcmToken])
                rollback()
            }
        }

        @Test
        fun `unlinkAllByUser는 사용자의 모든 기기 연결만 끊고 다른 사용자는 그대로 둔다`() {
            transaction {
                insertUsers()
                devicePersistencePort.saveToken(command("fid-a", "token-a"), ME)
                devicePersistencePort.saveToken(command("fid-b", "token-b"), ME)
                devicePersistencePort.saveToken(command("fid-o", "token-o"), OTHER)

                devicePersistencePort.unlinkAllByUser(ME)
                devicePersistencePort.unlinkAllByUser(ME)

                assertEquals(0L, DeviceTable.selectAll().where { DeviceTable.userId eq ME }.count())
                assertEquals("token-a", row("fid-a")[DeviceTable.fcmToken])
                assertEquals("token-b", row("fid-b")[DeviceTable.fcmToken])
                assertEquals(OTHER, row("fid-o")[DeviceTable.userId])
                rollback()
            }
        }

        @Test
        fun `탈퇴한 사용자의 늦은 활동 기록은 user_id를 남기지 않는다`() {
            transaction {
                insertUsers()
                devicePersistencePort.recordActivity("fid-late", TODAY, null, null)
                devicePersistencePort.unlinkAllByUser(ME)
                UserTable.update({ UserTable.id eq ME }) { it[deletedAt] = OLD }

                devicePersistencePort.recordActivity("fid-late", TODAY, ME, "android")
                devicePersistencePort.recordActivity("fid-late-new", TODAY, ME, "android")

                assertNull(activity("fid-late")[DeviceActivityTable.userId])
                assertEquals("android", activity("fid-late")[DeviceActivityTable.platform])
                assertNull(activity("fid-late-new")[DeviceActivityTable.userId])
                rollback()
            }
        }

        @Test
        fun `clearTokens는 그 토큰의 행만 토큰을 비우고 연결과 다른 행은 남긴다`() {
            transaction {
                insertUsers()
                devicePersistencePort.saveToken(command("fid-bad", "token-bad"), ME)
                devicePersistencePort.saveToken(command("fid-ok", "token-ok"), ME)

                devicePersistencePort.clearTokens(listOf("token-bad", "token-unknown"))

                assertNull(row("fid-bad")[DeviceTable.fcmToken])
                assertEquals(ME, row("fid-bad")[DeviceTable.userId])
                assertEquals("token-ok", row("fid-ok")[DeviceTable.fcmToken])
                rollback()
            }
        }

        @Test
        fun `같은 계정에 연결된 기기는 로그인 상태로 토큰이 바뀌어도 동의를 유지한다`() {
            transaction {
                insertUsers()
                devicePersistencePort.saveToken(command("fid-rotate", "token-old"), ME)
                devicePersistencePort.updateConsent("fid-rotate", AGREED_AT, AGREED_AT)
                devicePersistencePort.clearTokens(listOf("token-old"))

                devicePersistencePort.saveToken(command("fid-rotate", "token-new"), ME)

                assertEquals(DeviceConsent(AGREED_AT, AGREED_AT), devicePersistencePort.findConsent("fid-rotate"))
                rollback()
            }
        }

        @Test
        fun `다른 계정이나 비로그인 경로로 토큰이 바뀌면 동의를 지운다`() {
            transaction {
                insertUsers()
                devicePersistencePort.saveToken(command("fid-other", "token-1"), ME)
                devicePersistencePort.updateConsent("fid-other", AGREED_AT, AGREED_AT)
                devicePersistencePort.saveToken(command("fid-other", "token-2"), OTHER)
                assertEquals(DeviceConsent.NONE, devicePersistencePort.findConsent("fid-other"))

                devicePersistencePort.saveToken(command("fid-guest", "token-3"), ME)
                devicePersistencePort.updateConsent("fid-guest", AGREED_AT, AGREED_AT)
                devicePersistencePort.saveToken(command("fid-guest", "token-4"), null)
                assertEquals(DeviceConsent.NONE, devicePersistencePort.findConsent("fid-guest"))
                rollback()
            }
        }

        @Test
        fun `lockConsent는 행이 없으면 토큰 없는 빈 행을 만들고 미동의를 돌려준다`() {
            transaction {
                assertEquals(LockedDevice(null, DeviceConsent.NONE), devicePersistencePort.lockConsent("fid-lock"))

                val row = row("fid-lock")
                assertNull(row[DeviceTable.fcmToken])
                assertNull(row[DeviceTable.userId])
                rollback()
            }
        }

        @Test
        fun `updateConsent로 저장한 동의를 findConsent로 읽고 행이 없으면 null이다`() {
            transaction {
                assertNull(devicePersistencePort.findConsent("fid-consent"))
                devicePersistencePort.lockConsent("fid-consent")
                val marketing = java.time.Instant.parse("2026-01-01T00:00:00Z")
                val night = java.time.Instant.parse("2026-01-02T00:00:00Z")

                devicePersistencePort.updateConsent("fid-consent", marketing, night)

                assertEquals(DeviceConsent(marketing, night), devicePersistencePort.findConsent("fid-consent"))
                devicePersistencePort.updateConsent("fid-consent", null, null)
                assertEquals(DeviceConsent.NONE, devicePersistencePort.findConsent("fid-consent"))
                rollback()
            }
        }

        // devices.user_id 는 users(id) FK 라 소유자 행이 먼저 있어야 한다(테스트 트랜잭션과 함께 롤백).
        @Test
        fun `findMarketingTokens는 토큰과 광고 동의가 있는 기기만, 야간이면 야간 동의까지 있는 기기만 돌려준다`() {
            transaction {
                DeviceTable.deleteAll()
                devicePersistencePort.saveToken(command("fid-day", "token-day"), null)
                devicePersistencePort.updateConsent("fid-day", AGREED_AT, null)
                devicePersistencePort.saveToken(command("fid-night", "token-night"), null)
                devicePersistencePort.updateConsent("fid-night", AGREED_AT, AGREED_AT)
                devicePersistencePort.saveToken(command("fid-no-consent", "token-no-consent"), null)
                devicePersistencePort.lockConsent("fid-no-token")
                devicePersistencePort.updateConsent("fid-no-token", AGREED_AT, AGREED_AT)

                assertEquals(
                    setOf("token-day", "token-night"),
                    devicePersistencePort.findMarketingTokens(false).toSet(),
                )
                assertEquals(listOf("token-night"), devicePersistencePort.findMarketingTokens(true))
                rollback()
            }
        }

        private fun insertUsers() {
            TransactionManager.current().exec(
                "INSERT INTO users (id, nickname) VALUES ($ME, '기기주인'), ($OTHER, '다른사용자')",
            )
        }

        private fun command(
            deviceId: String,
            token: String,
        ) = RegisterDeviceTokenCommand(deviceId, token)

        private fun row(deviceId: String) = DeviceTable.selectAll().where { DeviceTable.deviceId eq deviceId }.single()

        private fun activity(deviceId: String) =
            DeviceActivityTable.selectAll().where { DeviceActivityTable.deviceId eq deviceId }.single()

        private companion object {
            const val ME = 730101L
            const val OTHER = 730102L
            val OLD = Instant.parse("2000-01-01T00:00:00Z")
            val TODAY: java.time.LocalDate = java.time.LocalDate.parse("2026-11-09")
            val AGREED_AT: java.time.Instant = java.time.Instant.parse("2026-11-01T00:00:00Z")
        }
    }
