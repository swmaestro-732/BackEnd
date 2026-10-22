package com.example.backend.mobile.home.application.port.outbound

import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCoursePage
import com.example.backend.mobile.home.application.port.outbound.dto.HomeFeedCursor

/**
 * BFF 아웃바운드 포트 — 공개 코스 피드 후보 조회. 지금은 course 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 course 서비스 HTTP 클라이언트로 바꿔 끼운다.
 */
interface HomeFeedPort {
    /** 전체 공개(PUBLIC)·발행 코스 후보를 복합 키셋 [cursor] 이후부터 [size] 개 조회한다. */
    fun listPublicCandidates(
        cursor: HomeFeedCursor?,
        size: Int,
    ): HomeFeedCoursePage
}
