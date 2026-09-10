package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.UserEntity
import com.example.backend.user.adapter.outbound.persistence.exposed.UserTable
import com.example.backend.user.application.port.inbound.UserSummaryUseCase
import com.example.backend.user.application.port.outbound.UserProfileRow
import com.example.backend.user.domain.model.User
import com.example.backend.user.domain.model.UserStatus
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.Clock
import kotlin.time.toKotlinInstant

/**
 * users 테이블 접근 리포지토리 — 단순 조회는 DAO([UserEntity])로 하고, 쓰기·존재검사는 DSL 로 한다.
 * 엔티티의 도메인 변환은 어댑터가 담당하고, 도메인/애플리케이션 계층은 Exposed 를 전혀 알지 못한다.
 * 트랜잭션은 애플리케이션 서비스의 @Transactional 이 SpringTransactionManager 로 열어준다.
 */
@Repository
class UserRepository(
    private val clock: Clock,
) {
    internal fun findAll(): List<UserEntity> =
        UserEntity
            .find { UserTable.deletedAt.isNull() }
            .toList()

    internal fun findById(id: Long): UserEntity? =
        UserEntity
            .find { (UserTable.id eq id) and UserTable.deletedAt.isNull() }
            .singleOrNull()

    internal fun findByHandle(handle: String): UserEntity? =
        UserEntity
            .find { (UserTable.handle eq handle) and UserTable.deletedAt.isNull() }
            .singleOrNull()

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
                    bio = it.bio,
                    followersCnt = it.followersCnt,
                    followingsCnt = it.followingsCnt,
                    publicCoursesCnt = it.publicCoursesCnt,
                    followerCoursesCnt = it.followerCoursesCnt,
                    privateCoursesCnt = it.privateCoursesCnt,
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
                    bio = it.bio,
                    followersCnt = it.followersCnt,
                    followingsCnt = it.followingsCnt,
                    publicCoursesCnt = it.publicCoursesCnt,
                    followerCoursesCnt = it.followerCoursesCnt,
                    privateCoursesCnt = it.privateCoursesCnt,
                )
            }
    }

    /**
     * 공개범위별 코스 개수 캐시를 증감한다(0 델타 컬럼은 건드리지 않고, 하나라도 0 이 아니면 단일 UPDATE).
     * FollowRepository 의 followersCnt ±1 과 동일한 컬럼 증감 패턴.
     */
    fun applyCourseCountDelta(
        userId: Long,
        publicDelta: Int,
        followerDelta: Int,
        privateDelta: Int,
    ) {
        if (publicDelta == 0 && followerDelta == 0 && privateDelta == 0) return
        UserTable.update({ UserTable.id eq userId }) {
            if (publicDelta != 0) it[publicCoursesCnt] = publicCoursesCnt + publicDelta
            if (followerDelta != 0) it[followerCoursesCnt] = followerCoursesCnt + followerDelta
            if (privateDelta != 0) it[privateCoursesCnt] = privateCoursesCnt + privateDelta
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

    internal fun save(user: User): UserEntity =
        UserEntity
            .new {
                nickname = user.nickname
                profileImageUrl = user.profileImageUrl
                bio = user.bio
            }.also { it.refresh(flush = true) }

    fun update(user: User) {
        val id = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." }
        // 탈퇴 계정 프로필이 갱신되지 않도록 softDelete/reactivate 와 동일하게 deletedAt 필터를 건다(방어).
        val updated =
            UserTable.update({ (UserTable.id eq id) and UserTable.deletedAt.isNull() }) {
                it[nickname] = user.nickname
                it[handle] = user.handle
                it[profileImageUrl] = user.profileImageUrl
                it[bio] = user.bio
            }
        check(updated == 1) { "갱신할 활성 사용자를 찾지 못했습니다: id=$id" }
    }

    fun lockActive(userIds: List<Long>): Set<Long> {
        if (userIds.isEmpty()) return emptySet()
        return UserTable
            .select(UserTable.id)
            .where { (UserTable.id inList userIds) and UserTable.deletedAt.isNull() }
            .orderBy(UserTable.id to SortOrder.ASC)
            .forUpdate()
            .mapTo(mutableSetOf()) { it[UserTable.id].value }
    }

    fun softDelete(user: User) {
        val id = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." }
        UserTable.update({ (UserTable.id eq id) and UserTable.deletedAt.isNull() }) {
            it[deletedAt] = clock.instant().toKotlinInstant()
            it[status] = user.status
            // handle 은 탈퇴 시 즉시 해제(NULL)해 재사용 가능하게 한다 — 죽은 계정이 핸들을 영구 점유하지 않도록.
            // handle 은 nullable 이고 UNIQUE 는 NULL 다중 허용이라, existsByHandle 은 NULL 아닌 값만 매칭돼 자연히 활성 행만 본다.
            it[handle] = null
            // 재가입(reactivate)이 "처음 계정처럼" 시작하도록 잔여 프로필·카운터를 함께 비운다.
            // 관계·코스·저장 등 소유 데이터는 UserService.withdraw 오케스트레이션이 앞서 정리한다.
            it[profileImageUrl] = null
            it[bio] = null
            it[followersCnt] = 0
            it[followingsCnt] = 0
            it[publicCoursesCnt] = 0
            it[followerCoursesCnt] = 0
            it[privateCoursesCnt] = 0
        }
    }

    // nickname 은 전역 UNIQUE(탈퇴자 포함)이므로 중복검사도 전역으로 본다(탈퇴자 값 예약 유지).
    // handle 은 탈퇴 시 NULL 로 해제되므로 이 전역 검사가 자연히 활성 행만 본다(해제된 handle 은 재사용 가능).
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

    // 소셜 자격증명 조회(findBySocial/findWithdrawnBySocial)와 소셜 저장(saveWithSocial)은 IdentityRepository 로 이관했다(SCRUM-466).

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

    internal fun reactivate(user: User): UserEntity {
        val id = checkNotNull(user.id) { "영속화된 User 는 id 를 가진다." }
        // 소셜 자격증명은 identity/oauth_credentials 가 그대로 들고 있으므로(SCRUM-466), 여기선 프로필·상태만 되살린다.
        val updated =
            UserTable.update({ (UserTable.id eq id) and UserTable.deletedAt.isNotNull() }) {
                // 재활성화는 항상 ACTIVE 로 되살린다(반환 도메인 객체의 고정 status 와 일치).
                it[status] = UserStatus.ACTIVE
                it[deletedAt] = null
                it[nickname] = user.nickname
                it[handle] = user.handle
                it[profileImageUrl] = user.profileImageUrl
                it[bio] = user.bio
            }
        check(updated == 1) { "재활성화할 탈퇴 계정을 찾지 못했습니다(동시 재활성화 가능): id=$id" }
        return UserEntity.findById(id) ?: error("재활성화 직후 사용자를 되읽지 못했습니다: id=$id")
    }
}
