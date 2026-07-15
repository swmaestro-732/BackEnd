package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import com.example.backend.user.application.port.outbound.UserSummaryPort
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.springframework.stereotype.Repository

/** 아웃바운드 어댑터 — [UserSummaryPort] 를 Exposed 로 구현한다. 탈퇴(soft delete) 사용자는 제외. */
@Repository
class UserSummaryPersistenceAdapter : UserSummaryPort {
    override fun findByIds(ids: Collection<Long>): List<UserSummaryUseCase.UserSummary> =
        UserTable
            .selectAll()
            .where { (UserTable.id inList ids) and (UserTable.deletedAt.isNull()) }
            .map {
                UserSummaryUseCase.UserSummary(
                    id = it[UserTable.id],
                    nickname = it[UserTable.nickname],
                    profileImageUrl = it[UserTable.profileImageUrl],
                )
            }
}
