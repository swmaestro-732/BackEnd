package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.UserTable
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import com.example.backend.user.application.port.outbound.UserProfileRow
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import com.example.backend.user.domain.model.UserStatus
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.Clock
import kotlin.time.toKotlinInstant

/**
 * users 테이블 접근 리포지토리 — 사용자 본문의 조회·삽입·갱신과 팔로우 카운터 증감만 담당한다.
 * 도메인 ↔ 테이블 행(row) 매핑도 여기서 하고, 어댑터/애플리케이션 계층은 Exposed 를 알지 못한다.
 */
@Repository
class UserRepository(
    private val clock: Clock,
) {
    fun findAll(): List<User> =
        UserTable
            .selectAll()
            .where { UserTable.deletedAt.isNull() }
            .map(::toDomain)

    fun findById(id: Long): User? =
        UserTable
            .selectAll()
            .where { (UserTable.id eq id) and UserTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let(::toDomain)

    fun findProfile(userId: Long): UserProfileRow? =
        UserTable
            .selectAll()
            .where { (UserTable.id eq userId) and UserTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let(::toProfileRow)

    fun findProfiles(userIds: List<Long>): List<UserProfileRow> {
        if (userIds.isEmpty()) return emptyList()
        return UserTable
            .selectAll()
            .where { (UserTable.id inList userIds) and UserTable.deletedAt.isNull() }
            .map(::toProfileRow)
    }

    fun findSummaries(ids: Collection<Long>): List<UserSummaryUseCase.UserSummary> =
        UserTable
            .selectAll()
            .where { (UserTable.id inList ids) and UserTable.deletedAt.isNull() }
            .map {
                UserSummaryUseCase.UserSummary(
                    id = it[UserTable.id],
                    nickname = it[UserTable.nickname],
                    profileImageUrl = it[UserTable.profileImageUrl],
                )
            }

    fun save(user: User): User {
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

    fun update(user: User) {
        val id = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." }
        UserTable.update({ UserTable.id eq id }) {
            it[nickname] = user.nickname
            it[handle] = user.handle
            it[profileImageUrl] = user.profileImageUrl
        }
    }

    fun softDelete(user: User) {
        val id = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." }
        UserTable.update({ (UserTable.id eq id) and UserTable.deletedAt.isNull() }) {
            it[deletedAt] = clock.instant().toKotlinInstant()
            it[status] = user.status.code
        }
    }

    // nickname·handle 은 전역 UNIQUE(탈퇴자 포함)이므로 중복검사도 전역으로 본다.
    // (탈퇴자의 값도 예약 유지 — DB 제약과 앱 검사 범위를 일치시켜 UNIQUE 위반 500 을 방지.)
    fun existsByNickname(nickname: String): Boolean =
        UserTable
            .selectAll()
            .where { UserTable.nickname eq nickname }
            .empty()
            .not()

    fun existsByHandle(handle: String): Boolean =
        UserTable
            .selectAll()
            .where { UserTable.handle eq handle }
            .empty()
            .not()

    // 재활성화 대상(자기 자신)은 제외하고 검사한다 — 탈퇴 행이 예약한 값을 그대로 재사용하도록 허용.
    fun existsByNicknameExcludingUser(
        nickname: String,
        excludeUserId: Long,
    ): Boolean =
        UserTable
            .selectAll()
            .where { (UserTable.nickname eq nickname) and (UserTable.id neq excludeUserId) }
            .empty()
            .not()

    fun existsByHandleExcludingUser(
        handle: String,
        excludeUserId: Long,
    ): Boolean =
        UserTable
            .selectAll()
            .where { (UserTable.handle eq handle) and (UserTable.id neq excludeUserId) }
            .empty()
            .not()

    fun findBySocial(
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

    fun findWithdrawnBySocial(
        provider: SocialProvider,
        socialId: String,
    ): User? =
        UserTable
            .selectAll()
            .where {
                (UserTable.socialProvider eq provider.name) and
                    (UserTable.socialId eq socialId) and
                    UserTable.deletedAt.isNotNull()
            }.singleOrNull()
            ?.let(::toDomain)

    fun saveWithSocial(user: User): User {
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

    fun reactivate(user: User): User {
        val id = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." }
        val provider = checkNotNull(user.socialProvider) { "소셜 제공자가 필요합니다." }
        val socialId = checkNotNull(user.socialId) { "소셜 식별자가 필요합니다." }
        val updated =
            UserTable.update({ (UserTable.id eq id) and UserTable.deletedAt.isNotNull() }) {
                it[status] = user.status.code
                it[deletedAt] = null
                it[nickname] = user.nickname
                it[handle] = user.handle
                it[profileImageUrl] = user.profileImageUrl
            }
        check(updated == 1) { "재활성화할 탈퇴 계정을 찾지 못했습니다(동시 재활성화 가능): id=$id" }
        return User.reconstitute(
            id = id,
            nickname = user.nickname,
            handle = user.handle,
            profileImageUrl = user.profileImageUrl,
            socialProvider = provider,
            socialId = socialId,
            status = UserStatus.ACTIVE,
        )
    }

    /** 팔로우 성사 시 팔로잉 대상의 팔로워 수를 +1 한다. */
    fun increaseFollowers(userId: Long) {
        UserTable.update({ UserTable.id eq userId }) { it[followersCnt] = followersCnt + 1 }
    }

    /** 언팔로우 시 팔로잉 대상의 팔로워 수를 -1 한다. */
    fun decreaseFollowers(userId: Long) {
        UserTable.update({ UserTable.id eq userId }) { it[followersCnt] = followersCnt - 1 }
    }

    /** 팔로우 성사 시 팔로워 본인의 팔로잉 수를 +1 한다. */
    fun increaseFollowings(userId: Long) {
        UserTable.update({ UserTable.id eq userId }) { it[followingsCnt] = followingsCnt + 1 }
    }

    /** 언팔로우 시 팔로워 본인의 팔로잉 수를 -1 한다. */
    fun decreaseFollowings(userId: Long) {
        UserTable.update({ UserTable.id eq userId }) { it[followingsCnt] = followingsCnt - 1 }
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
