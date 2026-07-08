package com.example.backend.member.adapter.outbound.persistence

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object AddedPlaceTable : Table("added_places") {
    val id = long("id").autoIncrement()
    val tracingCourseId = long("tracing_course_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
