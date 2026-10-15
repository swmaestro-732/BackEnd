package com.example.backend.bootstrap.security

import org.springframework.security.access.prepost.PreAuthorize

/**
 * 현재 사용자("나") 기준 엔드포인트 보호용 메서드 시큐리티 메타 애노테이션.
 *
 * `purpose == "access"` 인 JWT 로 인증된 요청만 통과시킨다.
 * - 익명(토큰 없음): `isAuthenticated()`=false → 거부 → 인증 진입점으로 라우팅되어 401.
 * - purpose 가 다른 JWT(예: 회원가입 토큰): 인증은 됐으나 조건 불충족 → 403.
 *
 * SpEL `and` 는 단락 평가라 익명 요청에서 token 접근 전에 걸러진다.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@PreAuthorize("isAuthenticated() and authentication.token.getClaimAsString('purpose') == 'access'")
annotation class AccessTokenRequired
