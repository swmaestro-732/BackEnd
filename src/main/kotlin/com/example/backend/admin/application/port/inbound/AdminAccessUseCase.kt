package com.example.backend.admin.application.port.inbound

/** 어드민 토큰을 쓰는 매 요청마다 아직 admins 에 남아 있는지 확인하는 인바운드 포트. 삭제 즉시 접근을 끊는다. */
interface AdminAccessUseCase {
    fun isAdmin(email: String): Boolean
}
