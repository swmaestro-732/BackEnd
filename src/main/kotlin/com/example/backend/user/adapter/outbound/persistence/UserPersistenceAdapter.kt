package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.adapter.outbound.persistence.exposed.repository.UserRepository
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.application.port.outbound.UserProfileRow
import com.example.backend.user.domain.model.User
import org.springframework.stereotype.Component

/**
 * 아웃바운드 어댑터 — [UserPersistencePort] 를 구현한다.
 * 실제 테이블 접근은 [UserRepository] 에 위임하고, DAO 엔티티를 도메인으로 변환해 유스케이스 계약을 맞춘다.
 */
@Component
class UserPersistenceAdapter(
    private val userRepository: UserRepository,
) : UserPersistencePort {
    override fun findAll(): List<User> = userRepository.findAll().map { it.toDomain() }

    override fun findById(id: Long): User? = userRepository.findById(id)?.toDomain()

    override fun lockActive(userIds: List<Long>): Set<Long> = userRepository.lockActive(userIds)

    override fun findByHandle(handle: String): User? = userRepository.findByHandle(handle)?.toDomain()

    override fun findProfile(userId: Long): UserProfileRow? = userRepository.findProfile(userId)

    override fun findProfiles(userIds: List<Long>): List<UserProfileRow> = userRepository.findProfiles(userIds)

    override fun save(user: User): User = userRepository.save(user).toDomain()

    override fun update(user: User) = userRepository.update(user)

    override fun applyCourseCountDelta(
        userId: Long,
        publicDelta: Int,
        followerDelta: Int,
        privateDelta: Int,
    ) = userRepository.applyCourseCountDelta(userId, publicDelta, followerDelta, privateDelta)

    override fun softDelete(user: User) = userRepository.softDelete(user)

    override fun existsByNickname(nickname: String): Boolean = userRepository.existsByNickname(nickname)

    override fun existsByHandle(handle: String): Boolean = userRepository.existsByHandle(handle)

    override fun existsByNicknameExcludingUser(
        nickname: String,
        excludeUserId: Long,
    ): Boolean = userRepository.existsByNicknameExcludingUser(nickname, excludeUserId)

    override fun existsByHandleExcludingUser(
        handle: String,
        excludeUserId: Long,
    ): Boolean = userRepository.existsByHandleExcludingUser(handle, excludeUserId)

    override fun reactivate(user: User): User = userRepository.reactivate(user).toDomain()
}
