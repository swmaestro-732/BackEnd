package com.example.backend.user.adapter.inbound.web.request

import com.example.backend.user.application.dto.CreateUserCommand
import com.example.backend.user.domain.model.User
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 웹 요청 DTO. 애플리케이션 경계 타입([CreateUserCommand])으로 매핑한다. */
data class CreateUserRequest(
    @field:NotBlank
    @field:Size(max = User.MAX_NICKNAME_LENGTH)
    val nickname: String,
) {
    fun toCommand(): CreateUserCommand = CreateUserCommand(nickname = nickname)
}
