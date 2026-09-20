package com.example.backend.course.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CourseErrorCode
import com.example.backend.common.response.PlaceErrorCode
import com.example.backend.course.application.port.inbound.PlanUseCase
import com.example.backend.course.application.port.inbound.dto.CreatePlanCommand
import com.example.backend.course.application.port.inbound.dto.EditPlanCommand
import com.example.backend.course.application.port.inbound.dto.PlanPlaceCommand
import com.example.backend.course.application.port.inbound.dto.toPlan
import com.example.backend.course.application.port.outbound.CoursePersistencePort
import com.example.backend.course.application.port.outbound.PlaceLookupPort
import com.example.backend.course.application.port.outbound.PlanPersistencePort
import com.example.backend.course.domain.model.Plan
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 계획 쓰기(커맨드) 유스케이스 — 생성·편집·삭제. 조회는 [PlanQueryService] 가 담당한다.
 * 불변식(장소 1곳 이상·orderNo 중복 금지)은 [Plan] 이, 장소 존재는 [PlaceLookupPort](ACL)가,
 * 원본 코스 존재는 같은 도메인의 [CoursePersistencePort] 가 확인한다.
 */
@Service
@Transactional
class PlanService(
    private val planPersistencePort: PlanPersistencePort,
    private val coursePersistencePort: CoursePersistencePort,
    private val placeLookupPort: PlaceLookupPort,
) : PlanUseCase {
    override fun create(command: CreatePlanCommand): Plan {
        command.sourceCourseId?.let(::requireCourseExists)
        requirePlacesExist(command.places)
        return planPersistencePort.save(command.toPlan())
    }

    override fun edit(command: EditPlanCommand): Plan {
        val existingPlan = requireOwnedPlan(command.planId, command.userId)
        requirePlacesExist(command.places)
        return planPersistencePort.update(command.toPlan(existingPlan))
    }

    override fun delete(
        userId: Long,
        planId: Long,
    ) {
        requireOwnedPlan(planId, userId)
        // deleted_at IS NULL 가드라 동시 이중 삭제는 0행 — 이미 지워진 결과와 같으므로 그대로 성공 처리한다.
        planPersistencePort.softDelete(planId)
    }

    override fun deleteAllByOwner(userId: Long) {
        // 회원 탈퇴 정리 — 소유자의 살아있는 계획을 전부 소프트 삭제한다(코스 deleteAllByAuthor 와 같은 방식).
        planPersistencePort.softDeleteAllByOwner(userId)
    }

    private fun requireCourseExists(courseId: Long) {
        if (!coursePersistencePort.existsById(courseId)) {
            throw BusinessException(CourseErrorCode.COURSE_NOT_FOUND, "원본 코스를 찾을 수 없습니다: id=$courseId")
        }
    }

    private fun requirePlacesExist(places: List<PlanPlaceCommand>) {
        val requestedIds = places.map { it.placeId }.distinct()
        if (placeLookupPort.findPlacesByIds(requestedIds).size != requestedIds.size) {
            throw BusinessException(PlaceErrorCode.PLACE_NOT_FOUND)
        }
    }

    /** 계획을 조회하고 소유 정책([Plan.ensureOwned])을 통과시킨다 — 없음·삭제·타인 소유 모두 404. */
    private fun requireOwnedPlan(
        planId: Long,
        userId: Long,
    ): Plan {
        val plan = planPersistencePort.findById(planId) ?: throw BusinessException(CourseErrorCode.PLAN_NOT_FOUND)
        Plan.ensureOwned(ownerId = plan.userId, requesterId = userId)
        return plan
    }
}
