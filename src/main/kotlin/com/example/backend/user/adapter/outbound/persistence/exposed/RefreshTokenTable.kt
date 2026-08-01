package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** V4__auth.sql 의 refresh_tokens 매핑. FK 정의는 Flyway 가 소유한다. */
internal object RefreshTokenTable : Table("refresh_tokens") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val tokenHash = varchar("token_hash", 255)
    val expiresAt = timestamp("expires_at")
    val revoked = bool("revoked")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
