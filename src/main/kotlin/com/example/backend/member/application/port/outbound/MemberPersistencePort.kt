package com.example.backend.member.application.port.outbound

import com.example.backend.member.domain.model.Member

/**
 * 아웃바운드 포트 — 애플리케이션이 영속성에 요구하는 계약.
 * 구현체(Exposed 어댑터)는 adapter/outbound/persistence 에 위치한다.
 */
interface MemberPersistencePort {
    fun findAll(): List<Member>

    /** 저장 후 식별자가 부여된 Member 를 반환한다. */
    fun save(member: Member): Member
}
