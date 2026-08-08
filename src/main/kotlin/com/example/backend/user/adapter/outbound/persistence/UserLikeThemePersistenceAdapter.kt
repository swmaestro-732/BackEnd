package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.UserLikeThemeRepository
import com.example.backend.user.application.port.outbound.UserLikeThemePort
import org.springframework.stereotype.Component

/** 아웃바운드 어댑터 — [UserLikeThemePort] 를 구현한다. 실제 테이블 접근은 [UserLikeThemeRepository] 에 위임한다. */
@Component
class UserLikeThemePersistenceAdapter(
    private val userLikeThemeRepository: UserLikeThemeRepository,
) : UserLikeThemePort {
    override fun replaceLikeThemes(
        userId: Long,
        themes: List<String>,
    ) = userLikeThemeRepository.replaceLikeThemes(userId, themes)

    override fun findLikeThemes(userId: Long): List<String> = userLikeThemeRepository.findThemesByUserId(userId)
}
