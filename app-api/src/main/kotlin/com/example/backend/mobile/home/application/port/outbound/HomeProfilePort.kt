package com.example.backend.mobile.home.application.port.outbound

import com.example.backend.mobile.home.application.port.outbound.dto.HomeProfile

/**
 * BFF 아웃바운드 포트 — 홈 헤더 프로필 조회. 지금은 user 도메인 인바운드 포트에 위임하는 어댑터가 구현하지만,
 * MSA 분리 후엔 user 서비스 HTTP 클라이언트로 바꿔 끼운다.
 */
interface HomeProfilePort {
    /** 닉네임과 프로필 이미지. 탈퇴 등으로 사용자가 없으면 null. */
    fun findProfile(userId: Long): HomeProfile?
}
