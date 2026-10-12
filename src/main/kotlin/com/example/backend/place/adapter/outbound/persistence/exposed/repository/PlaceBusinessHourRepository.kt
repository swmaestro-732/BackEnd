package com.example.backend.place.adapter.outbound.persistence.exposed.repository

import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceBusinessHourTable
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceEntity
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceTable
import com.example.backend.place.domain.model.PlaceBusinessHour
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import kotlin.time.Clock

/** place_business_hours 와 places.business_hours_synced_at 접근 리포지토리. */
@Repository
class PlaceBusinessHourRepository {
    /** 주어진 id 중 business_hours_synced_at IS NULL 인 활성 장소. */
    internal fun findUnsynced(placeIds: List<Long>): List<PlaceEntity> {
        if (placeIds.isEmpty()) return emptyList()
        return PlaceEntity
            .find { (PlaceTable.id inList placeIds) and unsynced() }
            .toList()
    }

    /** business_hours_synced_at IS NULL 인 활성 장소를 id 오름차순으로 afterId 다음부터 limit개. */
    internal fun findUnsynced(
        afterId: Long?,
        limit: Int,
    ): List<PlaceEntity> =
        PlaceEntity
            .find { (PlaceTable.id greater (afterId ?: 0L)) and unsynced() }
            .orderBy(PlaceTable.id to SortOrder.ASC)
            .limit(limit)
            .toList()

    /** 요일별 행을 지우고 다시 넣은 뒤 동기화 시각을 기록한다. */
    fun replaceBusinessHours(
        placeId: Long,
        hours: List<PlaceBusinessHour>,
    ) {
        PlaceBusinessHourTable.deleteWhere { PlaceBusinessHourTable.placeId eq placeId }
        PlaceBusinessHourTable.batchInsert(hours) { hour ->
            this[PlaceBusinessHourTable.placeId] = placeId
            this[PlaceBusinessHourTable.dayOfWeek] = hour.dayOfWeek.toShort()
            this[PlaceBusinessHourTable.openTime] = hour.openTime
            this[PlaceBusinessHourTable.closeTime] = hour.closeTime
        }
        PlaceTable.update({ PlaceTable.id eq placeId }) {
            it[businessHoursSyncedAt] = Clock.System.now()
        }
    }

    private fun unsynced() = PlaceTable.businessHoursSyncedAt.isNull() and PlaceTable.deletedAt.isNull()
}
