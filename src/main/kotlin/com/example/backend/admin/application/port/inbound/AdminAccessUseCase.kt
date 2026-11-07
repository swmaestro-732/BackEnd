package com.example.backend.admin.application.port.inbound

/**
 * 어드민 토큰을 쓰는 매 요청마다 아직 admins 에 남아 있고 토큰의 구글 계정에 묶여 있는지 확인하는 인바운드 포트.
 * 행을 지우거나 다른 계정으로 다시 묶으면 이전 토큰도 즉시 끊긴다.
 */
interface AdminAccessUseCase {
    fun isAdmin(
        email: String,
        googleSub: String,
    ): Boolean
}
