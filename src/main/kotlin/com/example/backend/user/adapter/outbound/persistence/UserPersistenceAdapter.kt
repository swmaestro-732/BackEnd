package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.UserRepository
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.application.port.outbound.UserProfileRow
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [UserPersistencePort] 를 구현한다.
 * 실제 테이블 접근·도메인 매핑은 [UserRepository] 에 위임하고, 이 어댑터는 유스케이스 계약만 맞춘다.
 */
@Component
class UserPersistenceAdapter(
    private val userRepository: UserRepository,
) : UserPersistencePort {
    override fun findAll(): List<User> = userRepository.findAll()

    override fun findById(id: Long): User? = userRepository.findById(id)

    override fun findByHandle(handle: String): User? = userRepository.findByHandle(handle)

    override fun findProfile(userId: Long): UserProfileRow? = userRepository.findProfile(userId)

    override fun save(user: User): User = userRepository.save(user)

    override fun update(user: User) = userRepository.update(user)

    override fun softDelete(user: User) = userRepository.softDelete(user)

    override fun existsByNickname(nickname: String): Boolean = userRepository.existsByNickname(nickname)

    override fun existsByHandle(handle: String): Boolean = userRepository.existsByHandle(handle)

    override fun findBySocial(
        provider: SocialProvider,
        socialId: String,
    ): User? = userRepository.findBySocial(provider, socialId)

    override fun findWithdrawnBySocial(
        provider: SocialProvider,
        socialId: String,
    ): User? = userRepository.findWithdrawnBySocial(provider, socialId)

    override fun existsByNicknameExcludingUser(
        nickname: String,
        excludeUserId: Long,
    ): Boolean = userRepository.existsByNicknameExcludingUser(nickname, excludeUserId)

    override fun existsByHandleExcludingUser(
        handle: String,
        excludeUserId: Long,
    ): Boolean = userRepository.existsByHandleExcludingUser(handle, excludeUserId)

    override fun saveWithSocial(user: User): User = userRepository.saveWithSocial(user)

    override fun reactivate(user: User): User = userRepository.reactivate(user)
}
