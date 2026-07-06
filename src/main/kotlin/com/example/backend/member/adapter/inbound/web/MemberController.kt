package com.example.backend.member.adapter.inbound.web

import com.example.backend.member.adapter.inbound.web.request.CreateMemberRequest
import com.example.backend.member.adapter.inbound.web.response.MemberResponse
import com.example.backend.member.application.port.inbound.MemberUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 인바운드 어댑터 — HTTP 요청을 인바운드 포트([MemberUseCase]) 호출로 변환한다.
 * Request → Command, Result → Response 로 매핑해 도메인/애플리케이션 타입을 밖으로 노출하지 않는다.
 */
@RestController
@RequestMapping("/api/members")
class MemberController(
    private val memberUseCase: MemberUseCase,
) {
    @GetMapping
    fun list(): List<MemberResponse> = memberUseCase.list().map(MemberResponse::from)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateMemberRequest,
    ): MemberResponse = MemberResponse.from(memberUseCase.create(request.toCommand()))
}
