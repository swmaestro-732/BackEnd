package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — 관심 테마로 고를 수 있는 코스 카테고리 목록. 정본은 course 도메인 소유라
 * user 는 이 포트로만 접근하고 어댑터가 course 인바운드에 위임한다.
 * MSA 분리 시 어댑터만 course 서비스 클라이언트로 교체한다.
 */
interface LikeThemePort {
    /** 유효한 관심 테마(코스 카테고리) 이름 전체. */
    fun listThemeNames(): List<String>
}
