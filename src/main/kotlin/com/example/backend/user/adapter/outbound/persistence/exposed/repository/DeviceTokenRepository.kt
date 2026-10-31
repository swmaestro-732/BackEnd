package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.DeviceTokenTable
import com.example.backend.user.application.port.inbound.dto.RegisterDeviceTokenCommand
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.springframework.stereotype.Repository

@Repository
class DeviceTokenRepository {
    fun upsert(
        userId: Long,
        command: RegisterDeviceTokenCommand,
    ) {
        TransactionManager.current().exec(
            """
            INSERT INTO device_tokens (user_id, token, platform, app_build)
            VALUES (?, ?, ?, ?)
            ON CONFLICT ON CONSTRAINT uq_device_tokens_token DO UPDATE SET
                user_id = EXCLUDED.user_id,
                platform = EXCLUDED.platform,
                app_build = EXCLUDED.app_build,
                last_seen_at = now()
            """.trimIndent(),
            args =
                listOf(
                    DeviceTokenTable.userId.columnType to userId,
                    DeviceTokenTable.token.columnType to command.token,
                    DeviceTokenTable.platform.columnType to command.platform,
                    DeviceTokenTable.appBuild.columnType to command.appBuild,
                ),
        )
    }

    fun deleteByUserAndToken(
        userId: Long,
        token: String,
    ) {
        DeviceTokenTable.deleteWhere {
            (DeviceTokenTable.userId eq userId) and (DeviceTokenTable.token eq token)
        }
    }

    fun deleteAllByUser(userId: Long) {
        DeviceTokenTable.deleteWhere { DeviceTokenTable.userId eq userId }
    }
}
