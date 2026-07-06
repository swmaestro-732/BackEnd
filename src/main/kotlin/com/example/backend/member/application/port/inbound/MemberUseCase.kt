package com.example.backend.member.application.port.inbound

import com.example.backend.member.application.dto.CreateMemberCommand
import com.example.backend.member.application.dto.MemberResult

/**
 * 인바운드 포트 — 애플리케이션이 바깥(웹 등)에 제공하는 유스케이스 계약.
 * 입출력은 도메인이 아니라 애플리케이션 DTO(Command/Result)로 주고받는다.
 */
interface MemberUseCase {
    fun list(): List<MemberResult>

    fun create(command: CreateMemberCommand): MemberResult
}
