package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** V12 의 user_areas 매핑 — 회원가입·프로필 수정에서 선택한 관심 지역(법정동코드) 목록. */
internal object UserAreaTable : Table("user_areas") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val areaCode = varchar("area_code", 10)
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}
