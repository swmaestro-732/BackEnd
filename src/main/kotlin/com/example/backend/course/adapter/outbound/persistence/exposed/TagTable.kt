package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다 → DAO(TagEntity)·DSL 공용.
internal object TagTable : LongIdTable("tags") {
    val name = varchar("name", 50)
}

/** tags 테이블의 DAO 엔티티([TagTable] 과 한 쌍). 어댑터 밖으로 내보내지 않고 DTO 로 변환한다. */
internal class TagEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<TagEntity>(TagTable)

    var name by TagTable.name
}
