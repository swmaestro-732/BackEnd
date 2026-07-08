package com.example.backend.user.application.port.inbound

import com.example.backend.user.application.dto.CreateUserCommand
import com.example.backend.user.application.dto.UserResult

/**
 * 인바운드 포트 — 애플리케이션이 바깥(웹 등)에 제공하는 유스케이스 계약.
 * 입출력은 도메인이 아니라 애플리케이션 DTO(Command/Result)로 주고받는다.
 */
interface UserUseCase {
    fun list(): List<UserResult>

    fun create(command: CreateUserCommand): UserResult
}
