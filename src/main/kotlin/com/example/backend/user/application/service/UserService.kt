package com.example.backend.user.application.service

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.ErrorCode
import com.example.backend.user.application.dto.CreateUserCommand
import com.example.backend.user.application.dto.UserProfileResult
import com.example.backend.user.application.dto.UserResult
import com.example.backend.user.application.port.inbound.UserUseCase
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.domain.model.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 유스케이스 구현. 인바운드 포트([UserUseCase])를 구현하고,
 * 아웃바운드 포트([UserPersistencePort])에만 의존한다(구현 세부는 모른다).
 * 도메인 모델은 애플리케이션 밖으로 내보내지 않고 [UserResult] 로 매핑한다.
 */
@Service
@Transactional(readOnly = true)
class UserService(
    private val userPersistencePort: UserPersistencePort,
) : UserUseCase {
    override fun list(): List<UserResult> = userPersistencePort.findAll().map(UserResult::from)

    @Transactional
    override fun create(command: CreateUserCommand): UserResult {
        val saved = userPersistencePort.save(User.create(command.nickname))
        return UserResult.from(saved)
    }

    /**
     * MOCK(SCRUM-223): 프로필(handle·profileImageUrl)은 아직 도메인/스키마에 없어 고정 데이터를 반환한다.
     * 실제 구현 시 User 도메인·스키마 확장 후 아웃바운드 포트로 조회하도록 교체한다.
     */
    override fun getProfile(userId: Long): UserProfileResult =
        MOCK_PROFILES[userId]
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=$userId")

    private companion object {
        /** id=1 만 성공 데이터, 나머지는 404 */
        val MOCK_PROFILES: Map<Long, UserProfileResult> =
            mapOf(
                1L to
                    UserProfileResult(
                        id = 1L,
                        nickname = "지호님",
                        handle = "jiho_routes",
                        profileImageUrl =
                            "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQi7ZSFKA2brmDYt72J8vLDQxgOJKxs-lj4tavhXo_pEA&s=10",
                        isFollowing = false,
                        isFollower = true,
                    ),
            )
    }
}
