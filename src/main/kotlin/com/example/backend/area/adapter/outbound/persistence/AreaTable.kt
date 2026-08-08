package com.example.backend.area.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table

/**
 * area 테이블(법정동 마스터)의 Exposed DSL 정의. DAO(Active Record) 대신 순수 DSL 만 쓴다.
 * 이 Table 객체는 아웃바운드 어댑터 내부에만 존재하고, 도메인/애플리케이션 계층으로 새어나가지 않는다.
 *
 * 스키마는 Flyway(V12)가 소유한다 — SchemaUtils.create 로 만들지 않는다. PK 는 문자 코드(code)다.
 */
internal object AreaTable : Table("area") {
    val code = varchar("code", 10)
    val sidoName = varchar("sido_name", 20)
    val sigunguName = varchar("sigungu_name", 30).nullable()
    val dongName = varchar("dong_name", 40)
    val isActive = bool("is_active")

    // DB 가 GENERATED ALWAYS AS (SUBSTRING(code,1,5)) STORED 로 자동 채운다.
    // databaseGenerated() 를 붙여야 INSERT/UPDATE 문에서 제외된다
    // (없으면 "cannot insert a non-DEFAULT value into column" 에러). insert/update 블록에 절대 명시하지 않는다.
    val sigunguCode = varchar("sigungu_code", 5).databaseGenerated()

    override val primaryKey = PrimaryKey(code)
}
