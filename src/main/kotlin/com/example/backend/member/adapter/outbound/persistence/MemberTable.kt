package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

/**
 * Exposed 테이블 정의. 영속성 세부사항이므로 아웃바운드 어댑터 내부에만 둔다.
 * 실제 스키마는 Flyway(V2__member.sql)가 생성한다.
 */
internal object MemberTable : Table("members") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val area = varchar("area", 50)
    override val primaryKey = PrimaryKey(id)
}
