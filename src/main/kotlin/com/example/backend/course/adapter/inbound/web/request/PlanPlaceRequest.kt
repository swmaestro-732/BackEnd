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
    /**
     * 다음 장소까지 도보 소요 시간(분). 코스([CreateCoursePlaceRequest])와 같은 세 가지 값을 받는다:
     * - 정상: 0 이상의 분
     * - [UNREACHABLE_ON_FOOT](-1): 걸어서 갈 수 없는 구간
     * - null: 마지막 장소(다음 장소가 없음)
     *
     * 그래서 `@Positive` 가 아니라 `@Min(-1)` 로 하한만 막는다. "마지막 장소만 null" 은 강제하지 않는다 —
     * 클라이언트가 중간 구간을 미측정 상태로 보낼 수 있어서다.
     */
    @field:Min(UNREACHABLE_ON_FOOT)
    val walkingMinutes: Int? = null,
) {
    fun toCommand(): PlanPlaceCommand =
        PlanPlaceCommand(placeId = placeId, orderNo = orderNo, memo = memo, walkingMinutes = walkingMinutes)

    companion object {
        /** plan_places.order_no 가 SMALLINT 라 Short 범위까지만 받는다. */
        const val MAX_ORDER_NO = Short.MAX_VALUE.toLong()

        /** 도보 이동 불가 구간을 나타내는 센티널. 소요 시간이 아니므로 합계·표시에서 제외해야 한다. */
        const val UNREACHABLE_ON_FOOT = -1L
    }
}
