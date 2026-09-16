package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다. 삽입은 batchUpsert(DSL), 조회도 DSL 로만 접근한다.
internal object TagTable : LongIdTable("tags") {
    val name = varchar("name", 50)
}
