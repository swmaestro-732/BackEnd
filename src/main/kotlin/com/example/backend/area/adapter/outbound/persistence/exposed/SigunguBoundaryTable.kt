package com.example.backend.area.adapter.outbound.persistence.exposed

import com.example.backend.common.persistence.postgis.GeometryColumnType
import org.jetbrains.exposed.v1.core.Table

/** 스키마는 Flyway(V18)가 소유한다. geom 은 조회하지 않고 WHERE 조건에만 쓴다. */
internal object SigunguBoundaryTable : Table("sigungu_boundaries") {
    val sigunguCode = varchar("sigungu_code", 5)
    val geom = registerColumn("geom", GeometryColumnType)

    override val primaryKey = PrimaryKey(sigunguCode)
}
