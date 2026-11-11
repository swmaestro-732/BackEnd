package com.example.backend.user.application.service

import com.example.backend.user.application.port.inbound.UserInterestUseCase
import com.example.backend.user.application.port.inbound.dto.UserInterestResult
import com.example.backend.user.application.port.outbound.UserAreaPersistencePort
import com.example.backend.user.application.port.outbound.UserLikeThemePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** [UserInterestUseCase] 구현 — 관심 테마·지역 코드를 저장된 그대로 돌려준다(폐지된 지역 코드도 매칭에 쓰이지 않을 뿐 걸러내지 않는다). */
@Service
@Transactional(readOnly = true)
class UserInterestService(
    private val userLikeThemePort: UserLikeThemePort,
    private val userAreaPersistencePort: UserAreaPersistencePort,
) : UserInterestUseCase {
    override fun findInterests(userId: Long): UserInterestResult =
        UserInterestResult(
            likeThemes = userLikeThemePort.findLikeThemes(userId),
            areaCodes = userAreaPersistencePort.findAreaCodes(userId),
        )
}
