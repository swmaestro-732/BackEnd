package com.example.backend.mobile.course.application.port.outbound

/**
 * BFF 아웃바운드 포트 — 코스별 저장수 조회. 지금은 user 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 user 서비스 HTTP 클라이언트로 바꿔 끼운다.
 */
interface FeedSavesPort {
    /** 코스별 저장수(courseId → count). 저장 기록 없는 코스는 맵에서 빠진다(호출측이 0 으로 처리). */
    fun countSaves(courseIds: List<Long>): Map<Long, Int>
}
