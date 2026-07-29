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
                    // 온보딩 완료(핸들 보유) 유저만 팔로우·작성자로 요약되므로 nickname 은 사실상 non-null.
                    // 방어적으로 빈 문자열 폴백 — nullable 을 UserSummary 전체로 전파하지 않는다.
                    nickname = it[UserTable.nickname] ?: "",
                    profileImageUrl = it[UserTable.profileImageUrl],
                )
            }
}
