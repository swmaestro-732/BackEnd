package com.example.backend.course.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

// 삽입은 batchInsert(DSL), 조회도 DSL 로만 접근한다.
internal object PlanPlaceTable : LongIdTable("plan_places") {
    val planId = long("plan_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val orderNo = short("order_no")
    val memo = varchar("memo", 500).nullable()
}
