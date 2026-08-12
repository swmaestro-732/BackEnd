package com.example.backend.user.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock

// LongIdTable 이 id·primaryKey 를 제공한다. (tracing_course_id, place_id) 는 uq 유니크라 insertIgnore 로 멱등 체크인.
internal object AddedPlaceTable : LongIdTable("added_places") {
    val tracingCourseId = long("tracing_course_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val createdAt = timestamp("created_at").clientDefault { Clock.System.now() }
}
