package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.PlanPlaceTable
import com.example.backend.course.domain.model.PlanPlace
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/** plan_places 테이블 접근 리포지토리 — 계획에 담긴 장소의 삽입·삭제·조회. */
@Repository
class PlanPlaceRepository {
    fun insertAll(
        planId: Long,
        places: List<PlanPlace>,
    ) {
        if (places.isEmpty()) return
        PlanPlaceTable.batchInsert(places, shouldReturnGeneratedValues = false) { place ->
            this[PlanPlaceTable.planId] = planId
            this[PlanPlaceTable.placeId] = place.placeId
            this[PlanPlaceTable.orderNo] = place.orderNo.toShort()
            this[PlanPlaceTable.memo] = place.memo
        }
    }

    /** 계획의 모든 장소를 지운다(전체 치환 편집 전처리). */
    fun deleteByPlanId(planId: Long) {
        PlanPlaceTable.deleteWhere { PlanPlaceTable.planId eq planId }
    }

    /** 계획의 장소들을 orderNo 오름차순으로 읽는다. */
    fun findByPlanId(planId: Long): List<PlanPlace> =
        PlanPlaceTable
            .selectAll()
            .where { PlanPlaceTable.planId eq planId }
            .orderBy(PlanPlaceTable.orderNo to SortOrder.ASC)
            .map {
                PlanPlace(
                    placeId = it[PlanPlaceTable.placeId],
                    orderNo = it[PlanPlaceTable.orderNo].toInt(),
                    memo = it[PlanPlaceTable.memo],
                )
            }

    /** 여러 계획의 장소 수를 planId 별로 한 번에 센다(목록 화면용). 장소가 없는 계획은 결과에 빠진다. */
    fun countByPlanIds(planIds: List<Long>): Map<Long, Int> {
        if (planIds.isEmpty()) return emptyMap()
        val placeCount = PlanPlaceTable.id.count()
        return PlanPlaceTable
            .select(PlanPlaceTable.planId, placeCount)
            .where { PlanPlaceTable.planId inList planIds }
            .groupBy(PlanPlaceTable.planId)
            .associate { it[PlanPlaceTable.planId] to it[placeCount].toInt() }
    }
}
