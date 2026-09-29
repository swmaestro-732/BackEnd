package com.example.backend.course.adapter.inbound.web.request

import com.example.backend.course.application.port.inbound.dto.EditPlanCommand
import com.example.backend.course.domain.model.Plan
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDate

/** 계획 편집 요청 */
data class EditPlanRequest(
    @field:Size(max = Plan.MAX_TITLE_LENGTH)
    val title: String = "",
    @field:Size(max = Plan.MAX_MEMO_LENGTH)
    val memo: String? = null,
    val plannedDate: LocalDate? = null,
    @field:Valid
    @field:Size(max = Plan.MAX_PLACES)
    val places: List<PlanPlaceRequest> = emptyList(),
) {
    fun toCommand(
        userId: Long,
        planId: Long,
    ): EditPlanCommand =
        EditPlanCommand(
            planId = planId,
            userId = userId,
            title = title,
            memo = memo,
            plannedDate = plannedDate,
            places = places.map(PlanPlaceRequest::toCommand),
        )
}
