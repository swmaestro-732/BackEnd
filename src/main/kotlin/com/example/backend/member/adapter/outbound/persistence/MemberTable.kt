package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

/**
 * Exposed 테이블 정의. 영속성 세부사항이므로 아웃바운드 어댑터 내부에만 둔다.
 * 실제 스키마는 Flyway(V2__schema.sql)가 생성한다. users 의 나머지 컬럼은 DB 기본값을 사용하므로
 * 여기서는 member 도메인이 다루는 id·nickname 만 선언한다.
 */
internal object MemberTable : Table("users") {
    val id = long("id").autoIncrement()
    val nickname = varchar("nickname", 20)
    override val primaryKey = PrimaryKey(id)
}
