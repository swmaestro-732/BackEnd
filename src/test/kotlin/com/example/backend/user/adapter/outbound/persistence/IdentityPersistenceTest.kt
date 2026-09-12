package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.adapter.outbound.persistence.exposed.IdentityTable
import com.example.backend.user.adapter.outbound.persistence.exposed.OAuthCredentialTable
import com.example.backend.user.adapter.outbound.persistence.exposed.UserTable
import com.example.backend.user.application.port.outbound.IdentityPersistencePort
import com.example.backend.user.domain.model.Identity
import com.example.backend.user.domain.model.OAuthCredential
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import com.example.backend.user.domain.model.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import kotlin.time.toKotlinInstant

/**
 * IdentityRepository 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 * 새 identity/oauth_credentials 경로와 register() 의 3-row 삽입을 검증한다.
 * IdentityPersistenceAdapter 경유 호출로 어댑터도 함께 커버한다.
 * 각 테스트는 transaction { ... rollback() } 으로 격리한다.
 */
class IdentityPersistenceTest
    @Autowired
    constructor(
        private val port: IdentityPersistencePort,
    ) : IntegrationTestBase() {
        // ── findActiveUserByCredential ─────────────────────────────────────────

        @Test
        fun `findActiveUserByCredential - credential 과 identity 로 연결된 활성 유저를 반환한다`() {
            transaction {
                val (_, userId) = insertIdentityAndUser("활성조회1", "KAKAO", "kakao-active-it-1")

                val found = port.findActiveUserByCredential(SocialProvider.KAKAO, "kakao-active-it-1")

                assertThat(found).isNotNull()
                assertThat(found!!.id).isEqualTo(userId)
                assertThat(found.nickname).isEqualTo("활성조회1")
                rollback()
            }
        }

        @Test
        fun `findActiveUserByCredential - credential 이 없으면 null 을 반환한다`() {
            transaction {
                val found = port.findActiveUserByCredential(SocialProvider.KAKAO, "no-such-credential-it")

                assertThat(found).isNull()
                rollback()
            }
        }

        @Test
        fun `findActiveUserByCredential - 탈퇴(deletedAt 설정) 유저는 활성 조회에서 제외된다`() {
            transaction {
                insertIdentityAndUser("탈퇴조회1", "KAKAO", "kakao-withdrawn-active-it-1", withdrawn = true)

                val found = port.findActiveUserByCredential(SocialProvider.KAKAO, "kakao-withdrawn-active-it-1")

                assertThat(found).isNull()
                rollback()
            }
        }

        // ── findWithdrawnUserByCredential ──────────────────────────────────────

        @Test
        fun `findWithdrawnUserByCredential - credential 과 연결된 탈퇴 유저를 반환한다`() {
            transaction {
                val (_, userId) = insertIdentityAndUser("탈퇴조회2", "KAKAO", "kakao-withdrawn-it-1", withdrawn = true)

                val found = port.findWithdrawnUserByCredential(SocialProvider.KAKAO, "kakao-withdrawn-it-1")

                assertThat(found).isNotNull()
                assertThat(found!!.id).isEqualTo(userId)
                rollback()
            }
        }

        @Test
        fun `findWithdrawnUserByCredential - 탈퇴 유저가 없으면 null 을 반환한다`() {
            transaction {
                val found = port.findWithdrawnUserByCredential(SocialProvider.KAKAO, "no-withdrawn-it")

                assertThat(found).isNull()
                rollback()
            }
        }

        // ── register ──────────────────────────────────────────────────────────

        @Test
        fun `register 는 identity 와 credential 과 user 행을 함께 삽입하고 id 가 부여된 User 를 반환한다`() {
            transaction {
                val credential = OAuthCredential.create(SocialProvider.KAKAO, "kakao-register-it-1")
                val identity = Identity.create(credential)
                val primaryUser =
                    User.create(
                        nickname = "가입러1",
                        handle = "joiner_1",
                        profileImageUrl = null,
                    )

                val saved = port.register(identity, primaryUser)

                assertThat(saved.id).isNotNull()
                assertThat(saved.id!! > 0).isTrue()
                assertThat(saved.nickname).isEqualTo("가입러1")
                assertThat(saved.handle).isEqualTo("joiner_1")
                rollback()
            }
        }

        // ── helpers ───────────────────────────────────────────────────────────

        /** identity + oauth_credentials + users 를 삽입하고 (identityId, userId) 를 반환한다. */
        private fun insertIdentityAndUser(
            nickname: String,
            provider: String,
            socialId: String,
            withdrawn: Boolean = false,
        ): Pair<Long, Long> {
            val identityId = IdentityTable.insert { }[IdentityTable.id].value
            OAuthCredentialTable.insert {
                it[OAuthCredentialTable.identityId] = identityId
                it[OAuthCredentialTable.provider] = provider
                it[OAuthCredentialTable.socialId] = socialId
            }
            val userId =
                UserTable
                    .insert {
                        it[UserTable.nickname] = nickname
                        it[handle] = if (!withdrawn) "h_$nickname" else null
                        it[status] = if (withdrawn) UserStatus.WITHDRAWN else UserStatus.ACTIVE
                        it[followersCnt] = 0
                        it[followingsCnt] = 0
                        it[publicCoursesCnt] = 0
                        it[followerCoursesCnt] = 0
                        it[privateCoursesCnt] = 0
                        it[UserTable.identityId] = identityId
                        it[isPrimary] = true
                        if (withdrawn) it[deletedAt] = Instant.now().toKotlinInstant()
                    }[UserTable.id]
                    .value
            return identityId to userId
        }
    }
