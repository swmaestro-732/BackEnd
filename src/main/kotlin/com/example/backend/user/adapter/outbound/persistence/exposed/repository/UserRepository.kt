package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.UserEntity
import com.example.backend.user.adapter.outbound.persistence.exposed.UserTable
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import com.example.backend.user.application.port.outbound.UserProfileRow
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import com.example.backend.user.domain.model.UserStatus
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.Clock
import kotlin.time.toKotlinInstant

/**
 * users 테이블 접근 리포지토리 — 도메인 ↔ 테이블 매핑을 여기서 담당한다.
 * 단순 조회는 DAO([UserEntity])로 하고, 쓰기·존재검사는 DSL 로 한다.
 * 어댑터는 이 리포지토리에 위임하고, 도메인/애플리케이션 계층은 Exposed 를 전혀 알지 못한다.
 * 트랜잭션은 애플리케이션 서비스의 @Transactional 이 SpringTransactionManager 로 열어준다.
 */
@Repository
class UserRepository(
    private val clock: Clock,
) {
    fun findAll(): List<User> =
        UserEntity
            .find { UserTable.deletedAt.isNull() }
            .map { it.toDomain() }

    fun findById(id: Long): User? =
        UserEntity
            .find { (UserTable.id eq id) and UserTable.deletedAt.isNull() }
            .singleOrNull()
            ?.toDomain()

    fun findByHandle(handle: String): User? =
        UserEntity
            .find { (UserTable.handle eq handle) and UserTable.deletedAt.isNull() }
            .singleOrNull()
            ?.toDomain()

    fun findProfile(userId: Long): UserProfileRow? =
        UserEntity
            .find { (UserTable.id eq userId) and UserTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let {
                UserProfileRow(
                    id = it.id.value,
                    nickname = it.nickname,
                    handle = it.handle,
                    profileImageUrl = it.profileImageUrl,
                    followersCnt = it.followersCnt,
                    followingsCnt = it.followingsCnt,
                    coursesCnt = it.coursesCnt,
                )
            }

    /** 여러 프로필을 한 번에 읽는다(탈퇴 제외). 없는·삭제된 id 는 결과에서 빠지며 순서는 보장하지 않는다. */
    fun findProfiles(userIds: List<Long>): List<UserProfileRow> {
        if (userIds.isEmpty()) return emptyList()
        return UserEntity
            .find { (UserTable.id inList userIds) and UserTable.deletedAt.isNull() }
            .map {
                UserProfileRow(
                    id = it.id.value,
                    nickname = it.nickname,
                    handle = it.handle,
                    profileImageUrl = it.profileImageUrl,
                    followersCnt = it.followersCnt,
                    followingsCnt = it.followingsCnt,
                    coursesCnt = it.coursesCnt,
                )
            }
    }

    /** 탈퇴(soft delete) 사용자는 제외하고 요약 정보만 읽는다. */
    fun findSummariesByIds(ids: Collection<Long>): List<UserSummaryUseCase.UserSummary> =
        UserEntity
            .find { (UserTable.id inList ids) and UserTable.deletedAt.isNull() }
            .map {
                UserSummaryUseCase.UserSummary(
                    id = it.id.value,
                    nickname = it.nickname,
                    profileImageUrl = it.profileImageUrl,
                )
            }

    fun save(user: User): User {
        val id =
            UserTable
                .insert {
                    it[nickname] = user.nickname
                    it[profileImageUrl] = user.profileImageUrl
                }[UserTable.id]
                .value
        return User.reconstitute(
            id = id,
            nickname = user.nickname,
            profileImageUrl = user.profileImageUrl,
        )
    }

    fun update(user: User) {
        val id = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." }
        // 탈퇴 계정 프로필이 갱신되지 않도록 softDelete/reactivate 와 동일하게 deletedAt 필터를 건다(방어).
        val updated =
            UserTable.update({ (UserTable.id eq id) and UserTable.deletedAt.isNull() }) {
                it[nickname] = user.nickname
                it[handle] = user.handle
                it[profileImageUrl] = user.profileImageUrl
            }
        check(updated == 1) { "갱신할 활성 사용자를 찾지 못했습니다: id=$id" }
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

    fun findBySocial(
        provider: SocialProvider,
        socialId: String,
    ): User? =
        UserEntity
            .find {
                (UserTable.socialProvider eq provider.name) and
                    (UserTable.socialId eq socialId) and
                    UserTable.deletedAt.isNull()
            }.singleOrNull()
            ?.toDomain()

    fun findWithdrawnBySocial(
        provider: SocialProvider,
        socialId: String,
    ): User? =
        UserEntity
            .find {
                (UserTable.socialProvider eq provider.name) and
                    (UserTable.socialId eq socialId) and
                    UserTable.deletedAt.isNotNull()
            }.singleOrNull()
            ?.toDomain()

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

    fun saveWithSocial(user: User): User {
        val provider = checkNotNull(user.socialProvider) { "소셜 제공자가 필요합니다." }
        val socialId = checkNotNull(user.socialId) { "소셜 식별자가 필요합니다." }
        val id =
            UserTable
                .insert {
                    it[nickname] = user.nickname
                    it[handle] = user.handle
                    it[profileImageUrl] = user.profileImageUrl
                    it[socialProvider] = provider.name
                    it[UserTable.socialId] = socialId
                }[UserTable.id]
                .value
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
                // 재활성화는 항상 ACTIVE 로 되살린다(반환 도메인 객체의 고정 status 와 일치).
                it[status] = UserStatus.ACTIVE.code
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
}
