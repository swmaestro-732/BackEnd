package com.example.backend.member.adapter.outbound.persistence

import com.example.backend.member.application.port.outbound.MemberPersistencePort
import com.example.backend.member.domain.model.Member
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/**
 * 아웃바운드 어댑터 — [MemberPersistencePort] 를 Exposed 로 구현한다.
 * 도메인 ↔ 테이블 행(row) 매핑을 여기서 담당하고, 도메인/애플리케이션 계층은
 * Exposed 를 전혀 알지 못한다. 트랜잭션은 애플리케이션 서비스의 @Transactional 이
 * SpringTransactionManager(exposed-spring-boot-starter)로 열어준다.
 */
@Repository
class MemberPersistenceAdapter : MemberPersistencePort {
    override fun findAll(): List<Member> = MemberTable.selectAll().map(::toDomain)

    override fun save(member: Member): Member {
        val id =
            MemberTable.insert {
                it[nickname] = member.nickname
            }[MemberTable.id]
        return Member.reconstitute(id = id, nickname = member.nickname)
    }

    private fun toDomain(row: ResultRow): Member =
        Member.reconstitute(
            id = row[MemberTable.id],
            nickname = row[MemberTable.nickname],
        )
}
