package com.example.backend.place.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.time

// LongIdTable 이 id(EntityID<Long>)·primaryKey 를 제공한다 → DAO(PlaceBusinessHourEntity)·DSL 공용.
internal object PlaceBusinessHourTable : LongIdTable("place_business_hours") {
    val placeId = long("place_id") // 같은 도메인(place) 참조 — 스키마에 FK 있음
    val dayOfWeek = short("day_of_week")
    val openTime = time("open_time").nullable()
    val closeTime = time("close_time").nullable()
}

/** place_business_hours 테이블의 DAO 엔티티([PlaceBusinessHourTable] 과 한 쌍). 어댑터 밖으로 내보내지 않고 DTO 로 변환한다. */
internal class PlaceBusinessHourEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<PlaceBusinessHourEntity>(PlaceBusinessHourTable)

    var placeId by PlaceBusinessHourTable.placeId
    var dayOfWeek by PlaceBusinessHourTable.dayOfWeek
    var openTime by PlaceBusinessHourTable.openTime
    var closeTime by PlaceBusinessHourTable.closeTime
}
