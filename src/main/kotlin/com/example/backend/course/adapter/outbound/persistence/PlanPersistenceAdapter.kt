package com.example.backend.course.adapter.outbound.persistence

import com.example.backend.course.adapter.outbound.persistence.exposed.repository.PlanPlaceRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.PlanRepository
import com.example.backend.course.adapter.outbound.persistence.exposed.repository.PlanRow
import com.example.backend.course.application.port.outbound.PlanCursor
import com.example.backend.course.application.port.outbound.PlanPersistencePort
import com.example.backend.course.application.port.outbound.PlanSummaryRow
import com.example.backend.course.domain.model.Plan
import com.example.backend.course.domain.model.PlanPlace
import kotlinx.datetime.toJavaLocalDate
import org.springframework.stereotype.Component
import kotlin.time.toJavaInstant

/**
 * 아웃바운드 어댑터 — [PlanPersistencePort] 를 구현한다.
 * 테이블 접근은 [PlanRepository]·[PlanPlaceRepository] 에 위임하고, 여기서는 저장 순서(plans → plan_places)만 조율한다.
 * 편집(전체 치환)은 본문을 갱신하고 기존 장소를 지운 뒤 요청 값으로 다시 심는다. 트랜잭션 경계는 서비스가 소유한다.
 */
@Component
class PlanPersistenceAdapter(
    private val planRepository: PlanRepository,
    private val planPlaceRepository: PlanPlaceRepository,
) : PlanPersistencePort {
    override fun findById(planId: Long): Plan? =
        planRepository.findById(planId)?.let { it.toDomain(planPlaceRepository.findByPlanId(planId)) }

    override fun findSummariesByOwner(
        userId: Long,
        cursor: PlanCursor?,
        size: Int,
    ): List<PlanSummaryRow> {
        val rows = planRepository.findByOwner(userId, cursor, size)
        val placeCounts = planPlaceRepository.countByPlanIds(rows.map { it.id })
        return rows.map {
            PlanSummaryRow(
                id = it.id,
                title = it.title,
                plannedDate = it.plannedDate?.toJavaLocalDate(),
                placeCount = placeCounts[it.id] ?: 0,
                updatedAt = it.updatedAt.toJavaInstant(),
            )
        }
    }

    override fun save(plan: Plan): Plan {
        val row = planRepository.insert(plan)
        planPlaceRepository.insertAll(row.id, plan.places)
        return row.toDomain(plan.places)
    }

    override fun update(plan: Plan): Plan? {
        val planId = checkNotNull(plan.id) { "영속화된 Plan 은 id 를 가진다." }
        // 0행(동시 소프트 삭제가 이긴 경우)은 null 로 돌려주고 404 판단은 서비스에 맡긴다.
        val row = planRepository.update(plan) ?: return null
        planPlaceRepository.deleteByPlanId(planId)
        planPlaceRepository.insertAll(planId, plan.places)
        return row.toDomain(plan.places)
    }

    override fun softDelete(planId: Long): Int = planRepository.softDelete(planId)

    override fun softDeleteAllByOwner(userId: Long): Int = planRepository.softDeleteAllByOwner(userId)

    private fun PlanRow.toDomain(places: List<PlanPlace>): Plan =
        Plan.reconstitute(
            id = id,
            userId = userId,
            title = title,
            memo = memo,
            plannedDate = plannedDate,
            sourceCourseId = sourceCourseId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            places = places,
        )
}
