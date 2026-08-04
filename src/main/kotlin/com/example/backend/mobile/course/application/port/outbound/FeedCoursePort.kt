package com.example.backend.mobile.course.application.port.outbound

import com.example.backend.mobile.course.application.port.outbound.dto.FeedCourse

/**
 * BFF 아웃바운드 포트 — 공개 코스 피드 후보 조회. 지금은 course 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 course 서비스 HTTP 클라이언트로 바꿔 끼운다.
 */
interface FeedCoursePort {
    /** 전체 공개(PUBLIC)·발행 코스 후보를 최신순으로 [limit] 개까지. 저장수 랭킹은 서비스가 조합한다. */
    fun listPublicCandidates(limit: Int): List<FeedCourse>
}
