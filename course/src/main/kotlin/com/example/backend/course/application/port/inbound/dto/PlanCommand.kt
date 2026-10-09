package com.example.backend.course.application.port.inbound.dto

import com.example.backend.course.domain.model.Plan
import com.example.backend.course.domain.model.PlanPlace
import kotlinx.datetime.toKotlinLocalDate
import java.time.LocalDate

/** 계획 생성 커맨드 — 인바운드 포트([com.example.backend.course.application.port.inbound.PlanUseCase]) 입력. */
data class CreatePlanCommand(
    val userId: Long,
    val title: String,
    val memo: String?,
    val plannedDate: LocalDate?,
    /** 계획 복제 원본 코스 id. 자유 계획이면 null. */
    val sourceCourseId: Long?,
    val places: List<PlanPlaceCommand>,
)

/** 계획 편집 커맨드(전체 치환). 원본 코스는 편집으로 바꿀 수 없어 받지 않는다. */
data class EditPlanCommand(
    val planId: Long,
    val userId: Long,
    val title: String,
    val memo: String?,
    val plannedDate: LocalDate?,
    val places: List<PlanPlaceCommand>,
)

data class PlanPlaceCommand(
    val placeId: Long,
    val orderNo: Int,
    val memo: String?,
    /** 다음 장소까지 도보 소요(분). -1 은 도보 불가, null 은 마지막 장소. */
    val walkingMinutes: Int?,
)

fun List<PlanPlaceCommand>.toPlanPlaces(): List<PlanPlace> =
    map {
        PlanPlace(
            placeId = it.placeId,
            orderNo = it.orderNo,
            memo = it.memo,
            walkingMinutes = it.walkingMinutes,
        )
    }

fun CreatePlanCommand.toPlan(): Plan =
    Plan.create(
        userId = userId,
        title = title,
        memo = memo,
        plannedDate = plannedDate?.toKotlinLocalDate(),
        sourceCourseId = sourceCourseId,
        places = places.toPlanPlaces(),
    )

/** 편집 커맨드 → 도메인 [Plan]. 원본 코스는 저장된 계획([existing])의 값을 그대로 잇는다. */
fun EditPlanCommand.toPlan(existing: Plan): Plan =
    Plan.edit(
        id = planId,
        userId = userId,
        title = title,
        memo = memo,
        plannedDate = plannedDate?.toKotlinLocalDate(),
        sourceCourseId = existing.sourceCourseId,
        places = places.toPlanPlaces(),
    )
