package com.example.backend.place.adapter.outbound.persistence.exposed.repository

import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceBusinessHourTable
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceEntity
import com.example.backend.place.adapter.outbound.persistence.exposed.PlaceTable
import com.example.backend.place.domain.model.PlaceBusinessHour
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.jdbc.batchInsert
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

    /** 미동기화 활성 장소를 FOR UPDATE SKIP LOCKED 로 잠근다 — 다른 트랜잭션(다른 서버 포함)이 잡고 있으면 null. */
    internal fun lockUnsynced(placeId: Long): PlaceEntity? =
        PlaceEntity
            .find { (PlaceTable.id eq placeId) and unsynced() }
            .forUpdate(ForUpdateOption.PostgreSQL.ForUpdate(ForUpdateOption.PostgreSQL.MODE.SKIP_LOCKED))
            .singleOrNull()

    /**
     * 요일별 행을 넣고 동기화 시각을 같은 트랜잭션에서 기록한다 — 둘은 항상 함께 남는다.
     * 호출부가 [lockUnsynced] 로 잠근 미동기화 장소만 넘기므로 기존 행은 없다.
     */
    fun saveBusinessHours(
        placeId: Long,
        hours: List<PlaceBusinessHour>,
    ) {
        PlaceBusinessHourTable.batchInsert(hours) { hour ->
            this[PlaceBusinessHourTable.placeId] = placeId
            this[PlaceBusinessHourTable.dayOfWeek] = hour.dayOfWeek.toShort()
            this[PlaceBusinessHourTable.openTime] = hour.openTime
            this[PlaceBusinessHourTable.closeTime] = hour.closeTime
            this[PlaceBusinessHourTable.breakStart] = hour.breakStart
            this[PlaceBusinessHourTable.breakEnd] = hour.breakEnd
        }
        PlaceTable.update({ PlaceTable.id eq placeId }) {
            it[businessHoursSyncedAt] = Clock.System.now()
        }
    }

    private fun unsynced() = PlaceTable.businessHoursSyncedAt.isNull() and PlaceTable.deletedAt.isNull()
}
