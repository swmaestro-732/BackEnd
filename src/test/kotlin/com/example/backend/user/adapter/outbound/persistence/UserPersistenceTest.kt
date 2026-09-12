package com.example.backend.user.adapter.outbound.persistence

import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.adapter.outbound.persistence.exposed.UserTable
import com.example.backend.user.adapter.outbound.persistence.exposed.repository.UserRepository
import com.example.backend.user.application.port.outbound.UserPersistencePort
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
 * UserRepository / UserPersistenceAdapter 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 * SCRUM-466 에서 추가된 reactivate, existsByNicknameExcludingUser, existsByHandleExcludingUser 와
 * findSummariesByIds, applyCourseCountDelta zero-delta 단락 경로를 커버한다.
 * [port](UserPersistenceAdapter 경유)와 [userRepository] 직접 호출로 어댑터와 리포지토리를 함께 커버한다.
 * 각 테스트는 transaction { ... rollback() } 으로 격리한다.
 */
class UserPersistenceTest
    @Autowired
    constructor(
        private val port: UserPersistencePort,
        private val userRepository: UserRepository,
    ) : IntegrationTestBase() {
        // ── reactivate ────────────────────────────────────────────────────────

        @Test
        fun `reactivate 는 탈퇴 사용자를 새 프로필로 되살리고 ACTIVE 상태를 반환한다`() {
            transaction {
                val userId = insertWithdrawnUser("재가입러1")
                val withdrawn =
                    User.reconstitute(id = userId, nickname = "재가입러1", handle = null, status = UserStatus.WITHDRAWN)
                val toReactivate = withdrawn.reactivate("새닉네임", "new_handle_r1", null)

                val result = port.reactivate(toReactivate)

                assertThat(result.id).isEqualTo(userId)
                assertThat(result.status).isEqualTo(UserStatus.ACTIVE)
                assertThat(result.nickname).isEqualTo("새닉네임")
                assertThat(result.handle).isEqualTo("new_handle_r1")
                rollback()
            }
        }

        // ── existsByNicknameExcludingUser ─────────────────────────────────────

        @Test
        fun `existsByNicknameExcludingUser 는 자기 자신 id 를 제외하면 false 를 반환한다`() {
            transaction {
                val userId = insertUser("자기닉1")

                assertThat(port.existsByNicknameExcludingUser("자기닉1", userId)).isFalse()
                rollback()
            }
        }

        @Test
        fun `existsByNicknameExcludingUser 는 다른 사용자가 같은 닉네임을 쓰면 true 를 반환한다`() {
            transaction {
                val userId1 = insertUser("중복닉1")
                val userId2 = insertUser("다른닉2")

                assertThat(port.existsByNicknameExcludingUser("중복닉1", userId2)).isTrue()
                rollback()
            }
        }

        // ── existsByHandleExcludingUser ───────────────────────────────────────

        @Test
        fun `existsByHandleExcludingUser 는 자기 자신 id 를 제외하면 false 를 반환한다`() {
            transaction {
                val userId = insertUser("핸들유저1")

                assertThat(port.existsByHandleExcludingUser("h_핸들유저1", userId)).isFalse()
                rollback()
            }
        }

        @Test
        fun `existsByHandleExcludingUser 는 다른 사용자가 같은 핸들을 쓰면 true 를 반환한다`() {
            transaction {
                val userId1 = insertUser("핸들유저2")
                val userId2 = insertUser("핸들유저3")

                assertThat(port.existsByHandleExcludingUser("h_핸들유저2", userId2)).isTrue()
                rollback()
            }
        }

        // ── findSummariesByIds ────────────────────────────────────────────────

        @Test
        fun `findSummariesByIds 는 빈 목록이면 즉시 빈 결과를 반환한다`() {
            transaction {
                val result = userRepository.findSummariesByIds(emptyList())

                assertThat(result).isEmpty()
                rollback()
            }
        }

        @Test
        fun `findSummariesByIds 는 id 목록에 해당하는 요약 정보를 반환한다`() {
            transaction {
                val userId = insertUser("요약대상1")

                val result = userRepository.findSummariesByIds(listOf(userId))

                assertThat(result).hasSize(1)
                assertThat(result[0].id).isEqualTo(userId)
                assertThat(result[0].nickname).isEqualTo("요약대상1")
                rollback()
            }
        }

        // ── findAll / save ────────────────────────────────────────────────────

        @Test
        fun `findAll 은 삭제되지 않은 유저 목록을 반환한다`() {
            transaction {
                val userId = insertUser("전체조회1")

                val all = port.findAll()

                assertThat(all.map { it.id }).contains(userId)
                rollback()
            }
        }

        @Test
        fun `save 는 새 User 를 저장하고 id 가 부여된 도메인 객체를 반환한다`() {
            transaction {
                val user = User.create(nickname = "저장러99")

                val saved = port.save(user)

                assertThat(saved.id).isNotNull()
                assertThat(saved.nickname).isEqualTo("저장러99")
                rollback()
            }
        }

        // ── applyCourseCountDelta ─────────────────────────────────────────────

        @Test
        fun `applyCourseCountDelta 는 모든 델타가 0 이면 DB 쿼리 없이 즉시 반환한다`() {
            transaction {
                val userId = insertUser("카운트유저1")

                // 예외 없이 통과하면 단락(early return) 경로 커버
                port.applyCourseCountDelta(userId, publicDelta = 0, followerDelta = 0, privateDelta = 0)
                rollback()
            }
        }

        // ── helpers ───────────────────────────────────────────────────────────

        private fun insertUser(nickname: String): Long =
            UserTable
                .insert {
                    it[UserTable.nickname] = nickname
                    it[handle] = "h_$nickname"
                    it[status] = UserStatus.ACTIVE
                    it[followersCnt] = 0
                    it[followingsCnt] = 0
                    it[publicCoursesCnt] = 0
                    it[followerCoursesCnt] = 0
                    it[privateCoursesCnt] = 0
                }[UserTable.id]
                .value

        private fun insertWithdrawnUser(nickname: String): Long =
            UserTable
                .insert {
                    it[UserTable.nickname] = nickname
                    it[handle] = null
                    it[status] = UserStatus.WITHDRAWN
                    it[followersCnt] = 0
                    it[followingsCnt] = 0
                    it[publicCoursesCnt] = 0
                    it[followerCoursesCnt] = 0
                    it[privateCoursesCnt] = 0
                    it[deletedAt] = Instant.now().toKotlinInstant()
                }[UserTable.id]
                .value
    }
