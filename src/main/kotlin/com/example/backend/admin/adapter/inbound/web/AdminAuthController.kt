package com.example.backend.admin.adapter.inbound.web

import com.example.backend.admin.adapter.inbound.web.request.AdminLoginRequest
import com.example.backend.admin.adapter.inbound.web.response.AdminLoginResponse
import com.example.backend.admin.adapter.inbound.web.response.AdminMeResponse
import com.example.backend.admin.adapter.inbound.web.response.AdminRegistrationResponse
import com.example.backend.admin.application.port.inbound.AdminAuthUseCase
import com.example.backend.admin.application.port.inbound.AdminNotRegisteredException
import com.example.backend.common.response.AdminErrorCode
import com.example.backend.common.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터: 어드민 인증.
 *
 * 로그인은 공개이고, 그 밖의 어드민 경로는 purpose=admin 토큰이면서 지금도 admins 에 있는 계정만 통과한다(SecurityConfig).
 */
@RestController
@RequestMapping("/admin/v1")
class AdminAuthController(
    private val adminAuthUseCase: AdminAuthUseCase,
) {
    /** 구글 ID 토큰으로 어드민 로그인. 등록되지 않은 계정은 403(4032) 과 본인 등록 정보(email, googleSub). */
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

    /** 등록되지 않은 계정: 403(4032) 에 본인 이메일과 구글 sub 를 담아 운영자에게 등록을 요청할 수 있게 한다. */
    @ExceptionHandler(AdminNotRegisteredException::class)
    fun handleNotRegistered(e: AdminNotRegisteredException): ResponseEntity<ApiResponse<AdminRegistrationResponse>> {
        val errorCode = AdminErrorCode.ADMIN_FORBIDDEN
        return ResponseEntity
            .status(errorCode.status)
            .body(ApiResponse(errorCode.code, errorCode.message, AdminRegistrationResponse(e.email, e.googleSub)))
    }
}
