package com.example.backend.course.adapter.inbound.web.request

import com.example.backend.course.application.port.inbound.dto.CreatePlanCommand
import com.example.backend.course.domain.model.Plan
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

/** 계획 생성 요청 */
data class CreatePlanRequest(
    @field:Size(max = Plan.MAX_TITLE_LENGTH)
    val title: String = "",
    @field:Size(max = Plan.MAX_MEMO_LENGTH)
    val memo: String? = null,
    val plannedDate: LocalDate? = null,
    @field:Positive
    val sourceCourseId: Long? = null,
    @field:Valid
    @field:Size(max = Plan.MAX_PLACES)
    val places: List<PlanPlaceRequest> = emptyList(),
) {
    fun toCommand(userId: Long): CreatePlanCommand =
        CreatePlanCommand(
            userId = userId,
            title = title,
            memo = memo,
            plannedDate = plannedDate,
            sourceCourseId = sourceCourseId,
            places = places.map(PlanPlaceRequest::toCommand),
        )
}
