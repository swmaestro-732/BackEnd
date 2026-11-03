package com.example.backend.admin.adapter.inbound.web

import com.example.backend.admin.adapter.inbound.web.request.AdminLoginRequest
import com.example.backend.admin.adapter.inbound.web.response.AdminLoginResponse
import com.example.backend.admin.adapter.inbound.web.response.AdminMeResponse
import com.example.backend.admin.application.port.inbound.AdminAuthUseCase
import com.example.backend.common.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터: 어드민 인증.
 *
 * 로그인은 공개이고, 그 밖의 어드민 경로는 purpose=admin 토큰만 통과한다(SecurityConfig).
 */
@RestController
@RequestMapping("/admin/v1")
class AdminAuthController(
    private val adminAuthUseCase: AdminAuthUseCase,
) {
    /** 구글 ID 토큰으로 어드민 로그인. admins 에 없는 계정은 403(4032). */
    @PostMapping("/auth/login")
    fun login(
        @Valid @RequestBody request: AdminLoginRequest,
    ): ApiResponse<AdminLoginResponse> =
        ApiResponse.success(AdminLoginResponse.from(adminAuthUseCase.login(request.idToken)))

    /** 현재 로그인한 어드민. subject = 이메일. */
    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal jwt: Jwt,
    ): ApiResponse<AdminMeResponse> = ApiResponse.success(AdminMeResponse(checkNotNull(jwt.subject)))
}
