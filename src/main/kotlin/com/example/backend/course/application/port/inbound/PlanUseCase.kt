package com.example.backend.course.application.port.inbound

import com.example.backend.course.application.port.inbound.dto.CreatePlanCommand
import com.example.backend.course.application.port.inbound.dto.EditPlanCommand
import com.example.backend.course.domain.model.Plan

/** 인바운드 포트 — 계획 쓰기(생성·편집·삭제). 소유자 본인만 가능하며 그 외는 404(PLAN_NOT_FOUND). */
interface PlanUseCase {
    fun create(command: CreatePlanCommand): Plan

    /** 전체 치환 편집. */
    fun edit(command: EditPlanCommand): Plan

    fun delete(
        userId: Long,
        planId: Long,
    )

    /** 소유자의 살아있는 계획을 전부 소프트 삭제한다 — 회원 탈퇴 정리용(user 도메인이 호출). */
    fun deleteAllByOwner(userId: Long)
}
