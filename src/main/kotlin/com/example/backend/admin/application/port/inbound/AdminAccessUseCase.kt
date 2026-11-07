package com.example.backend.admin.application.port.inbound

/**
 * 어드민 토큰을 쓰는 매 요청마다 토큰의 이메일과 구글 계정이 지금도 admins 에 등록돼 있는지 확인하는 인바운드 포트.
 * 행을 지우거나 등록 계정(google_sub)을 바꾸면 이전 토큰도 즉시 끊긴다.
 */
interface AdminAccessUseCase {
    fun isAdmin(
        email: String,
        googleSub: String,
    ): Boolean
}
