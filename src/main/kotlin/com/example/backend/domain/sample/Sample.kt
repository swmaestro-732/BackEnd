package com.example.backend.domain.sample

import org.jetbrains.exposed.v1.core.Table

/** Exposed 테이블 정의 (스키마는 Flyway V1__init.sql 가 생성). */
object Samples : Table("samples") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    override val primaryKey = PrimaryKey(id)
}

/** 도메인 모델. */
data class Sample(
    val id: Int,
    val name: String,
)
