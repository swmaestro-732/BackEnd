package com.example.backend.course.adapter.outbound.persistence.exposed.repository

import com.example.backend.course.adapter.outbound.persistence.exposed.PlanTable
import com.example.backend.course.application.port.outbound.PlanCursor
import com.example.backend.course.domain.model.Plan
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

/** plans 본문 한 행(장소 제외). 어댑터가 장소를 붙여 [Plan] 으로 복원한다. */
internal data class PlanRow(
    val id: Long,
    val userId: Long,
    val title: String,
    val memo: String?,
    val plannedDate: LocalDate?,
    val sourceCourseId: Long?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** plans 테이블 접근 리포지토리 */
@Repository
class PlanRepository {
    internal fun insert(plan: Plan): PlanRow {
        val now = Clock.System.now()
        val planId =
            PlanTable
                .insertAndGetId {
                    it[userId] = plan.userId
                    it[title] = plan.title
                    it[memo] = plan.memo
                    it[plannedDate] = plan.plannedDate
                    it[sourceCourseId] = plan.sourceCourseId
                    it[createdAt] = now
                    it[updatedAt] = now
                }.value
        return PlanRow(
            id = planId,
            userId = plan.userId,
            title = plan.title,
            memo = plan.memo,
            plannedDate = plan.plannedDate,
            sourceCourseId = plan.sourceCourseId,
            createdAt = now,
            updatedAt = now,
        )
    }

    /**
     * 계획 본문을 갱신하고 updated_at 을 새로 찍는다. 존재·소유권은 서비스가 사전 검증하므로
     * 여기서는 id·deleted_at IS NULL 로 원자 갱신만 하고, 0행(동시 소프트 삭제가 이긴 경우)이면 null 을 돌려준다.
     */
    internal fun update(plan: Plan): PlanRow? {
        val planId = checkNotNull(plan.id) { "영속화된 Plan 은 id 를 가진다." }
        val now = Clock.System.now()
        val affected =
            PlanTable.update({ (PlanTable.id eq planId) and PlanTable.deletedAt.isNull() }) {
                it[title] = plan.title
                it[memo] = plan.memo
                it[plannedDate] = plan.plannedDate
                it[updatedAt] = now
            }
        if (affected == 0) return null
        // 갱신하지 않은 컬럼(created_at·source_course_id)까지 담도록 확정 상태를 되읽는다.
        return findById(planId)
    }

    /** deleted_at IS NULL 인 행에만 소프트 삭제 스탬프를 찍는다. 반환은 영향받은 행 수(0 또는 1). */
    fun softDelete(planId: Long): Int {
        val now = Clock.System.now()
        return PlanTable.update({ (PlanTable.id eq planId) and PlanTable.deletedAt.isNull() }) {
            it[deletedAt] = now
            it[updatedAt] = now
        }
    }

    /** deleted_at IS NULL 인 계획 본문 한 행. */
    internal fun findById(planId: Long): PlanRow? =
        PlanTable
            .selectAll()
            .where { (PlanTable.id eq planId) and PlanTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let(::toRow)

    /**
     * 소유자의 미삭제 계획을 updatedAt DESC, id DESC 로 [size] 개까지 읽는다.
     * [cursor] 가 있으면 두 정렬 키가 가리키는 행보다 뒤에 있는 행만 조회한다(키셋).
     */
    internal fun findByOwner(
        userId: Long,
        cursor: PlanCursor?,
        size: Int,
    ): List<PlanRow> {
        var condition = (PlanTable.userId eq userId) and PlanTable.deletedAt.isNull()
        cursor?.let {
            val cursorUpdatedAt = it.updatedAt.toKotlinInstant()
            val afterCursor =
                (PlanTable.updatedAt less cursorUpdatedAt) or
                    ((PlanTable.updatedAt eq cursorUpdatedAt) and (PlanTable.id less it.id))
            condition = condition and afterCursor
        }
        return PlanTable
            .selectAll()
            .where(condition)
            .orderBy(PlanTable.updatedAt to SortOrder.DESC, PlanTable.id to SortOrder.DESC)
            .limit(size)
            .map(::toRow)
    }

    private fun toRow(it: ResultRow): PlanRow =
        PlanRow(
            id = it[PlanTable.id].value,
            userId = it[PlanTable.userId],
            title = it[PlanTable.title],
            memo = it[PlanTable.memo],
            plannedDate = it[PlanTable.plannedDate],
            sourceCourseId = it[PlanTable.sourceCourseId],
            createdAt = it[PlanTable.createdAt],
            updatedAt = it[PlanTable.updatedAt],
        )
}
