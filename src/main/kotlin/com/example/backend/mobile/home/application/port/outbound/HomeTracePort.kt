package com.example.backend.mobile.home.application.port.outbound

import com.example.backend.mobile.home.application.port.outbound.dto.HomeTracingCourse

/**
 * BFF 아웃바운드 포트 — 홈 "따라가는 중" 카드 조회. 지금은 course 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 course 서비스 HTTP 클라이언트로 바꿔 끼운다.
 */
interface HomeTracePort {
    /** 진행 중 따라가기와 그 코스. 없으면 null. 코스를 더 볼 수 없으면 isDeleted 로 표시한다. */
    fun findInProgress(userId: Long): HomeTracingCourse?
}
