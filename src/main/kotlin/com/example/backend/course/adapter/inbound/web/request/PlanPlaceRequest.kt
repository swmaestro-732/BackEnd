package com.example.backend.course.adapter.inbound.web.request

import com.example.backend.course.application.port.inbound.dto.PlanPlaceCommand
import com.example.backend.course.domain.model.Plan
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/** 계획에 담는 장소 한 곳 */
data class PlanPlaceRequest(
    @field:Positive
    val placeId: Long,
    @field:Min(0)
    @field:Max(MAX_ORDER_NO)
    val orderNo: Int,
    @field:Size(max = Plan.MAX_MEMO_LENGTH)
    val memo: String? = null,
) {
    fun toCommand(): PlanPlaceCommand = PlanPlaceCommand(placeId = placeId, orderNo = orderNo, memo = memo)

    companion object {
        /** plan_places.order_no 가 SMALLINT 라 Short 범위까지만 받는다. */
        const val MAX_ORDER_NO = Short.MAX_VALUE.toLong()
    }
}
