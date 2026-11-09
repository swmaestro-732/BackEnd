package com.example.backend.user.adapter.outbound.persistence.exposed

import com.example.backend.common.persistence.postgis.geographyPoint
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** V27 의 user_last_locations 매핑. 사용자별 최근 좌표 1건. */
internal object UserLastLocationTable : Table("user_last_locations") {
    val userId = long("user_id")
    val location = geographyPoint("location")
    val updatedAt = timestamp("updated_at").databaseGenerated()
    override val primaryKey = PrimaryKey(userId)
}
