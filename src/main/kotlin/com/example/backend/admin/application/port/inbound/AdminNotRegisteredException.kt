package com.example.backend.admin.application.port.inbound

/**
 * 구글 인증은 됐지만 admins 에 그 계정(이메일과 구글 sub)이 등록되지 않았다.
 * GlobalExceptionHandler 가 403(4032) 응답 data 로 [email], [googleSub] 를 돌려줘, 본인이 운영자에게 등록을 요청할 수 있게 한다.
 */
class AdminNotRegisteredException(
    val email: String,
    val googleSub: String,
) : RuntimeException("관리자 계정이 아닙니다.")
