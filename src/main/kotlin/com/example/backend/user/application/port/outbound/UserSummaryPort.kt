package com.example.backend.user.application.port.outbound

import com.example.backend.user.application.port.inbound.UserSummaryUseCase

/** 아웃바운드 포트 — 사용자 표시 정보(요약) 조회. 탈퇴/삭제된 사용자는 제외한다. */
interface UserSummaryPort {
    fun findByIds(ids: Collection<Long>): List<UserSummaryUseCase.UserSummary>
}
