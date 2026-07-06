package com.example.backend.member.application.service

import com.example.backend.common.area.Area
import com.example.backend.member.application.dto.CreateMemberCommand
import com.example.backend.member.application.dto.MemberResult
import com.example.backend.member.application.port.inbound.MemberUseCase
import com.example.backend.member.application.port.outbound.MemberPersistencePort
import com.example.backend.member.domain.model.Member
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 유스케이스 구현. 인바운드 포트([MemberUseCase])를 구현하고,
 * 아웃바운드 포트([MemberPersistencePort])에만 의존한다(구현 세부는 모른다).
 * 도메인 모델은 애플리케이션 밖으로 내보내지 않고 [MemberResult] 로 매핑한다.
 */
@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberPersistencePort: MemberPersistencePort,
) : MemberUseCase {
    override fun list(): List<MemberResult> = memberPersistencePort.findAll().map(MemberResult::from)

    @Transactional
    override fun create(command: CreateMemberCommand): MemberResult {
        val saved = memberPersistencePort.save(Member.create(command.name, Area(command.area)))
        return MemberResult.from(saved)
    }
}
