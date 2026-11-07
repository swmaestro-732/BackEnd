package com.example.backend.admin.application.port.inbound

/**
 * 구글 인증은 됐지만 admins 에 그 계정(이메일과 구글 sub)이 등록되지 않았다.
 * 등록하려면 운영자가 이 [email] 과 [googleSub] 를 admins 에 넣어야 하므로, 응답으로 본인에게 돌려준다.
 */
class AdminNotRegisteredException(
    val email: String,
    val googleSub: String,
) : RuntimeException("관리자 계정이 아닙니다.")
