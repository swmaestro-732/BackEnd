package com.example.backend.user.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/**
 * Exposed 테이블 정의. 영속성 세부사항이므로 아웃바운드 어댑터 내부에만 둔다.
 * 실제 스키마는 Flyway(V1__init.sql)가 생성한다. users 의 나머지 컬럼은 DB 기본값을 사용하므로
 * 여기서는 user 도메인이 다루는 컬럼만 선언한다(프로필 요약 조회에 필요한 컬럼 포함).
 */
internal object UserTable : Table("users") {
    val id = long("id").autoIncrement()
    val nickname = varchar("nickname", 20)
    val profileImageUrl = text("profile_image_url").nullable()
    val socialProvider = varchar("social_provider", 20).nullable()
    val socialId = varchar("social_id", 255).nullable()
    val deletedAt = timestamp("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
