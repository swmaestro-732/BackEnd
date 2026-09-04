package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.user.adapter.outbound.persistence.exposed.IdentityTable
import com.example.backend.user.adapter.outbound.persistence.exposed.OAuthCredentialTable
import com.example.backend.user.adapter.outbound.persistence.exposed.UserEntity
import com.example.backend.user.adapter.outbound.persistence.exposed.UserTable
import com.example.backend.user.domain.model.Identity
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.springframework.stereotype.Repository

/**
 * identities/oauth_credentials 테이블 접근 리포지토리(SCRUM-466 계정 분리).
 * 소셜 자격증명 → identity → 기본 User 조회와, 신규 가입 시 세 행을 함께 만드는 쓰기를 담당한다.
 * 도메인/애플리케이션 계층은 Exposed 를 전혀 알지 못하며, 트랜잭션은 서비스의 @Transactional 이 연다.
 */
@Repository
class IdentityRepository {
    fun findActiveUserByCredential(
        provider: SocialProvider,
        socialId: String,
    ): User? = findUserByCredential(provider, socialId, withdrawn = false)

    fun findWithdrawnUserByCredential(
        provider: SocialProvider,
        socialId: String,
    ): User? = findUserByCredential(provider, socialId, withdrawn = true)

    // credential(provider, socialId) → 같은 identity_id 를 가진 기본 프로필(is_primary) User 를 찾는다.
    // 멀티프로필 전 단계라 identity 당 프로필 1개=primary 이지만, 로그인 대상 선택 규칙을 미리 is_primary 로 고정한다.
    private fun findUserByCredential(
        provider: SocialProvider,
        socialId: String,
        withdrawn: Boolean,
    ): User? {
        val joined =
            OAuthCredentialTable
                .join(
                    UserTable,
                    JoinType.INNER,
                    onColumn = OAuthCredentialTable.identityId,
                    otherColumn = UserTable.identityId,
                ).select(UserTable.columns)
                .where {
                    (OAuthCredentialTable.provider eq provider.name) and
                        (OAuthCredentialTable.socialId eq socialId) and
                        UserTable.isPrimary and
                        (if (withdrawn) UserTable.deletedAt.isNotNull() else UserTable.deletedAt.isNull())
                }.limit(1)
                .firstOrNull()
        if (joined != null) return UserEntity.wrapRow(joined).toDomain()

        // 롤링 배포 호환 폴백(V5에서 제거): 구버전 인스턴스가 users.social_* 로만 만들어
        // identity_id·credential 이 없는 계정은 credential 조인으로 못 찾으므로 레거시 컬럼으로 조회한다.
        return findByLegacySocial(provider, socialId, withdrawn)
    }

    private fun findByLegacySocial(
        provider: SocialProvider,
        socialId: String,
        withdrawn: Boolean,
    ): User? =
        UserEntity
            .find {
                (UserTable.socialProvider eq provider.name) and
                    (UserTable.socialId eq socialId) and
                    (if (withdrawn) UserTable.deletedAt.isNotNull() else UserTable.deletedAt.isNull())
            }.firstOrNull()
            ?.toDomain()

    /** identity → 자격증명들 → 기본 User 를 순서대로 INSERT 하고 식별자가 부여된 User 를 반환한다. */
    fun register(
        identity: Identity,
        primaryUser: User,
    ): User {
        val identityId = IdentityTable.insert { }[IdentityTable.id].value
        identity.credentials.forEach { credential ->
            OAuthCredentialTable.insert {
                it[OAuthCredentialTable.identityId] = identityId
                it[provider] = credential.provider.name
                it[socialId] = credential.socialId
            }
        }
        // 로그인 대상이 되는 기본 프로필의 자격증명(멀티프로필 전이라 1개). 레거시 dual-write 에 쓴다.
        val primaryCredential = identity.credentials.firstOrNull()
        val userId =
            UserTable
                .insert {
                    it[nickname] = primaryUser.nickname
                    it[handle] = primaryUser.handle
                    it[profileImageUrl] = primaryUser.profileImageUrl
                    it[bio] = primaryUser.bio
                    it[UserTable.identityId] = identityId
                    it[isPrimary] = true
                    // 롤링 배포 호환 dual-write(V5에서 제거): 구버전 findBySocial(users.social_*)이 이 계정을 찾도록.
                    if (primaryCredential != null) {
                        it[socialProvider] = primaryCredential.provider.name
                        it[socialId] = primaryCredential.socialId
                    }
                }[UserTable.id]
                .value
        return User.reconstitute(
            id = userId,
            nickname = primaryUser.nickname,
            handle = primaryUser.handle,
            profileImageUrl = primaryUser.profileImageUrl,
            bio = primaryUser.bio,
        )
    }
}
