package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.course.application.port.inbound.PlanQueryUseCase
import com.example.backend.course.application.port.inbound.dto.PlanDetailResult
import com.example.backend.course.application.port.inbound.dto.PlanPlaceResult
import com.example.backend.course.application.port.inbound.dto.PlanSummary
import com.example.backend.course.application.port.inbound.dto.PlansQuery
import com.example.backend.course.application.port.inbound.dto.PlansResult
import com.example.backend.course.application.port.outbound.PlanCursor
import com.example.backend.course.application.port.outbound.PlanPersistencePort
import com.example.backend.course.domain.model.Plan
import kotlinx.datetime.toJavaLocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.toJavaInstant

/** 계획 조회(쿼리) 유스케이스 — 상세·내 목록. 소유자 본인 것만 돌려준다. */
@Service
@Transactional(readOnly = true)
class PlanQueryService(
    private val planPersistencePort: PlanPersistencePort,
) : PlanQueryUseCase {
    override fun getDetail(
        planId: Long,
        userId: Long,
    ): PlanDetailResult {
        val plan = planPersistencePort.findById(planId) ?: throw BusinessException(CourseErrorCode.PLAN_NOT_FOUND)
        Plan.ensureOwned(ownerId = plan.userId, requesterId = userId)
        return PlanDetailResult(
            id = requireNotNull(plan.id),
            title = plan.title,
            memo = plan.memo,
            plannedDate = plan.plannedDate?.toJavaLocalDate(),
            sourceCourseId = plan.sourceCourseId,
            createdAt = requireNotNull(plan.createdAt).toJavaInstant(),
            updatedAt = requireNotNull(plan.updatedAt).toJavaInstant(),
            places = plan.places.map { PlanPlaceResult(placeId = it.placeId, orderNo = it.orderNo, memo = it.memo) },
        )
    }

    override fun list(query: PlansQuery): PlansResult {
        val cursor = PlanCursorCodec.decode(query.cursor)
        // hasNext 판정을 위해 한 개 더 조회한 뒤 페이지 크기만큼 잘라낸다.
        val rows = planPersistencePort.findSummariesByOwner(query.userId, cursor, query.size + 1)
        val hasNext = rows.size > query.size
        val page = rows.take(query.size)
        return PlansResult(
            nextCursor =
                if (hasNext) {
                    page.last().let { PlanCursorCodec.encode(PlanCursor(updatedAt = it.updatedAt, id = it.id)) }
                } else {
                    null
                },
            hasNext = hasNext,
            plans =
                page.map {
                    PlanSummary(
                        id = it.id,
                        title = it.title,
                        plannedDate = it.plannedDate,
                        placeCount = it.placeCount,
                        updatedAt = it.updatedAt,
                    )
                },
        )
    }
}
