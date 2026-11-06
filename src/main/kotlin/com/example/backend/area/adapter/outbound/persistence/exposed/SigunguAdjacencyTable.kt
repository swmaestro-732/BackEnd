package com.example.backend.area.adapter.outbound.persistence.exposed

import org.jetbrains.exposed.v1.core.Table

internal object SigunguAdjacencyTable : Table("sigungu_adjacency") {
    val sigunguCode = varchar("sigungu_code", 5)
    val neighborCode = varchar("neighbor_code", 5)

    override val primaryKey = PrimaryKey(sigunguCode, neighborCode)
}
