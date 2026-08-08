package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.UserLikeTagRepository
import com.example.backend.user.application.port.outbound.UserLikeTagPort
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [UserLikeTagPort] 를 구현한다. 실제 테이블 접근은 [UserLikeTagRepository] 에 위임한다. */
@Component
class UserLikeTagPersistenceAdapter(
    private val userLikeTagRepository: UserLikeTagRepository,
) : UserLikeTagPort {
    override fun replaceLikeTags(
        userId: Long,
        tagIds: List<Long>,
    ) = userLikeTagRepository.replaceLikeTags(userId, tagIds)
}
