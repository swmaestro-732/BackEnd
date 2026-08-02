package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * V4__auth.sql 의 refresh_tokens 매핑. FK 정의는 Flyway 가 소유한다.
 * LongIdTable 이 id(EntityID<Long>, "id" 컬럼)와 primaryKey 를 제공한다 → DAO(RefreshTokenEntity)·DSL 공용.
 */
internal object RefreshTokenTable : LongIdTable("refresh_tokens") {
    val userId = long("user_id")
    val tokenHash = varchar("token_hash", 255)
    val expiresAt = timestamp("expires_at")
    val revoked = bool("revoked")
    val createdAt = timestamp("created_at")
}

/**
 * refresh_tokens 테이블의 DAO 엔티티([RefreshTokenTable] 과 한 쌍이라 같은 파일에 둔다).
 * 트랜잭션에 묶인 가변 영속 객체이므로 어댑터(outbound) 밖으로 내보내지 않고, 읽기 모델은 순수 DTO 로 변환해 반환한다.
 */
internal class RefreshTokenEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<RefreshTokenEntity>(RefreshTokenTable)

    var userId by RefreshTokenTable.userId
    var tokenHash by RefreshTokenTable.tokenHash
    var expiresAt by RefreshTokenTable.expiresAt
    var revoked by RefreshTokenTable.revoked
    var createdAt by RefreshTokenTable.createdAt
}
