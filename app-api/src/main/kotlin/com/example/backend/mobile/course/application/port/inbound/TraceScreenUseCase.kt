package com.example.backend.mobile.course.application.port.inbound

import com.example.backend.mobile.course.application.port.inbound.dto.TraceScreenResult

/** 인바운드 포트 — 따라가기 상세 화면 조합 (BFF). 진행 중이면 따라가기 화면, 완주면 기록 화면이 쓴다. */
interface TraceScreenUseCase {
    /** 내 세션만 조합한다 — 없음·타인(404) 판정은 course 도메인이 하고 그대로 올라온다. */
    fun getScreen(
        traceId: Long,
        userId: Long,
    ): TraceScreenResult
}
