package com.example.backend.user.adapter.outbound.persistence.exposed

import com.example.backend.user.domain.model.SavedPlaceCategory
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

internal object SavedPlaceTable : Table("saved_places") {
    val id = long("id").autoIncrement()
    val userId = long("user_id")
    val placeId = long("place_id") // cross-domain(place): FK 없음
    val category = enumerationByName<SavedPlaceCategory>("category", 50).nullable()
    val visited = bool("visited")
    val createdAt = timestamp("created_at")
    val deletedAt = timestamp("deleted_at").nullable() // 소프트 삭제 스탬프, NULL = 살아있음
    override val primaryKey = PrimaryKey(id)
}
