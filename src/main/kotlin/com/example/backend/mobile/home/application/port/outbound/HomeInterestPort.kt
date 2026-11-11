package com.example.backend.mobile.home.application.port.outbound

import com.example.backend.mobile.home.application.port.outbound.dto.HomeInterest

/**
 * BFF 아웃바운드 포트 — 추천 개인화용 관심 테마·지역 조회. 지금은 user 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 user 서비스 HTTP 클라이언트로 바꿔 끼운다.
 */
interface HomeInterestPort {
    fun findInterests(userId: Long): HomeInterest
}
