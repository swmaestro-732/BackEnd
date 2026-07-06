package com.example.backend.member.adapter.inbound.web.request

import com.example.backend.member.application.dto.CreateMemberCommand
import com.example.backend.member.domain.model.Member
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 웹 요청 DTO. 애플리케이션 경계 타입([CreateMemberCommand])으로 매핑한다. */
data class CreateMemberRequest(
    @field:NotBlank
    @field:Size(max = Member.MAX_NICKNAME_LENGTH)
    val nickname: String,
) {
    fun toCommand(): CreateMemberCommand = CreateMemberCommand(nickname = nickname)
}
