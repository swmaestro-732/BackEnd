package com.example.backend.user.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.CreateUserRequest
import com.example.backend.user.adapter.inbound.web.response.UserProfileResponse
import com.example.backend.user.adapter.inbound.web.response.UserResponse
import com.example.backend.user.application.port.inbound.UserUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — HTTP 요청을 인바운드 포트([UserUseCase]) 호출로 변환한다.
 * Request → Command, Result → Response 로 매핑해 도메인/애플리케이션 타입을 밖으로 노출하지 않는다.
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userUseCase: UserUseCase,
) {
    @GetMapping
    fun list(): ApiResponse<List<UserResponse>> = ApiResponse.success(userUseCase.list().map(UserResponse::from))

    @GetMapping("/{userId}")
    fun getProfile(
        @PathVariable userId: Long,
    ): ApiResponse<UserProfileResponse> = ApiResponse.success(UserProfileResponse.from(userUseCase.getProfile(userId)))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateUserRequest,
    ): ApiResponse<UserResponse> = ApiResponse.success(UserResponse.from(userUseCase.create(request.toCommand())))
}
