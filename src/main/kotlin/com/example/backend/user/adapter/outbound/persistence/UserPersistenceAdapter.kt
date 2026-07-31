package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.application.port.outbound.UserProfileRow
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import com.example.backend.user.domain.model.UserStatus
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.Clock
import kotlin.time.toKotlinInstant

/**
 * 아웃바운드 어댑터 — [UserPersistencePort] 를 Exposed 로 구현한다.
 * 도메인 ↔ 테이블 행(row) 매핑을 여기서 담당하고, 도메인/애플리케이션 계층은
 * Exposed 를 전혀 알지 못한다. 트랜잭션은 애플리케이션 서비스의 @Transactional 이
 * SpringTransactionManager(exposed-spring-boot-starter)로 열어준다.
 */
@Repository
class UserPersistenceAdapter(
    private val clock: Clock,
) : UserPersistencePort {
    override fun findAll(): List<User> =
        UserTable
            .selectAll()
            .where { UserTable.deletedAt.isNull() }
            .map(::toDomain)

    override fun findById(id: Long): User? =
        UserTable
            .selectAll()
            .where { (UserTable.id eq id) and UserTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let(::toDomain)

    override fun findProfile(userId: Long): UserProfileRow? =
        UserTable
            .selectAll()
            .where { (UserTable.id eq userId) and UserTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let(::toProfileRow)

    override fun findProfiles(userIds: List<Long>): List<UserProfileRow> {
        if (userIds.isEmpty()) return emptyList()
        return UserTable
            .selectAll()
            .where { (UserTable.id inList userIds) and UserTable.deletedAt.isNull() }
            .map(::toProfileRow)
    }

    override fun save(user: User): User {
        val id =
            UserTable.insert {
                it[nickname] = user.nickname
                it[profileImageUrl] = user.profileImageUrl
            }[UserTable.id]
        return User.reconstitute(
            id = id,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl,
        )
    }

    override fun update(user: User) {
        val id = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." }
        UserTable.update({ UserTable.id eq id }) {
            it[nickname] = user.nickname
            it[handle] = user.handle
            it[profileImageUrl] = user.profileImageUrl
        }
    }

    override fun softDelete(user: User) {
        val id = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." }
        UserTable.update({ (UserTable.id eq id) and UserTable.deletedAt.isNull() }) {
            it[deletedAt] = clock.instant().toKotlinInstant()
            it[status] = user.status.code
        }
    }

    // nickname·handle 은 전역 UNIQUE(탈퇴자 포함)이므로 중복검사도 전역으로 본다.
    // (탈퇴자의 값도 예약 유지 — DB 제약과 앱 검사 범위를 일치시켜 UNIQUE 위반 500 을 방지.)
    override fun existsByNickname(nickname: String): Boolean =
        UserTable
            .selectAll()
            .where { UserTable.nickname eq nickname }
            .empty()
            .not()

    override fun existsByHandle(handle: String): Boolean =
        UserTable
            .selectAll()
            .where { UserTable.handle eq handle }
            .empty()
            .not()

    override fun findBySocial(
        provider: SocialProvider,
        socialId: String,
    ): User? =
        UserTable
            .selectAll()
            .where {
                (UserTable.socialProvider eq provider.name) and
                    (UserTable.socialId eq socialId) and
                    UserTable.deletedAt.isNull()
            }.singleOrNull()
            ?.let(::toDomain)

    override fun saveWithSocial(user: User): User {
        val provider = checkNotNull(user.socialProvider) { "소셜 제공자가 필요합니다." }
        val socialId = checkNotNull(user.socialId) { "소셜 식별자가 필요합니다." }
        val id =
            UserTable.insert {
                it[nickname] = user.nickname
                it[handle] = user.handle
                it[profileImageUrl] = user.profileImageUrl
                it[socialProvider] = provider.name
                it[UserTable.socialId] = socialId
            }[UserTable.id]
        return User.reconstitute(
            id = id,
            nickname = user.nickname,
            handle = user.handle,
            profileImageUrl = user.profileImageUrl,
            socialProvider = provider,
            socialId = socialId,
        )
    }

    private fun toProfileRow(row: ResultRow): UserProfileRow =
        UserProfileRow(
            id = row[UserTable.id],
            nickname = row[UserTable.nickname],
            handle = row[UserTable.handle],
            profileImageUrl = row[UserTable.profileImageUrl],
            followersCnt = row[UserTable.followersCnt],
            followingsCnt = row[UserTable.followingsCnt],
            coursesCnt = row[UserTable.coursesCnt],
        )

    private fun toDomain(row: ResultRow): User =
        User.reconstitute(
            id = row[UserTable.id],
            nickname = row[UserTable.nickname],
            handle = row[UserTable.handle],
            profileImageUrl = row[UserTable.profileImageUrl],
            socialProvider = row[UserTable.socialProvider]?.let(SocialProvider::valueOf),
            socialId = row[UserTable.socialId],
            status = UserStatus.fromCode(row[UserTable.status]),
        )
}
