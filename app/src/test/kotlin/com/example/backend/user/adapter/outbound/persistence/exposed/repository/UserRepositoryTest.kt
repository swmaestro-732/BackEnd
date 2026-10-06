package com.example.backend.user.adapter.outbound.persistence.exposed.repository

import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.User
import com.example.backend.user.domain.model.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.atomic.AtomicLong

/**
 * [UserRepository] 영속성 통합 테스트(실제 PostgreSQL, [IntegrationTestBase]).
 *
 * 소셜 저장/조회, 카운트 델타 증감, 닉네임·핸들 중복검사(자기 제외 포함), 소프트 삭제→재활성화 왕복을
 * 검증한다. nickname 은 전역 UNIQUE 라 테스트마다 유니크한 값을 쓴다. transaction { ... rollback() } 로 격리한다.
 */
class UserRepositoryTest
    @Autowired
    constructor(
        private val repository: UserRepository,
    ) : IntegrationTestBase() {
        private val seq = AtomicLong(System.nanoTime() % 1_000_000)

        private fun uniq(prefix: String) = "$prefix${seq.incrementAndGet()}"

        @Test
        fun `saveWithSocial 로 저장하면 findBySocial 로 되읽고 다른 식별자는 못 찾는다`() {
            transaction {
                val nick = uniq("소셜유저")
                val socialId = uniq("kakao-sid-")
                val user =
                    User.createWithSocial(
                        nickname = nick,
                        profileImageUrl = "http://img/p.png",
                        socialProvider = SocialProvider.KAKAO,
                        socialId = socialId,
                        handle = uniq("h"),
                        bio = "소개",
                    )

                val saved = repository.saveWithSocial(user)
                assertThat(saved.id.value).isPositive()
                assertThat(saved.socialProvider).isEqualTo(SocialProvider.KAKAO.name)
                assertThat(saved.socialId).isEqualTo(socialId)

                val found = repository.findBySocial(SocialProvider.KAKAO, socialId)
                assertThat(found?.id?.value).isEqualTo(saved.id.value)
                assertThat(found?.nickname).isEqualTo(nick)

                assertThat(repository.findBySocial(SocialProvider.KAKAO, "없는sid")).isNull()
                assertThat(repository.findBySocial(SocialProvider.GOOGLE, socialId)).isNull()
                rollback()
            }
        }

        @Test
        fun `applyCourseCountDelta 는 0 이 아닌 컬럼만 증감하고 전부 0 이면 no-op 이다`() {
            transaction {
                val saved = repository.save(User.create(nickname = uniq("카운터유저")))
                val id = saved.id.value

                repository.applyCourseCountDelta(id, publicDelta = 2, followerDelta = 0, privateDelta = 1)
                var profile = repository.findProfile(id)!!
                assertThat(profile.publicCoursesCnt).isEqualTo(2)
                assertThat(profile.followerCoursesCnt).isEqualTo(0)
                assertThat(profile.privateCoursesCnt).isEqualTo(1)

                // 감소도 반영
                repository.applyCourseCountDelta(id, publicDelta = -1, followerDelta = 0, privateDelta = 0)
                profile = repository.findProfile(id)!!
                assertThat(profile.publicCoursesCnt).isEqualTo(1)

                // 전부 0 → UPDATE 자체를 실행하지 않는다(값 불변)
                repository.applyCourseCountDelta(id, 0, 0, 0)
                assertThat(repository.findProfile(id)!!.publicCoursesCnt).isEqualTo(1)
                rollback()
            }
        }

        @Test
        fun `닉네임과 핸들 중복검사는 자기 자신을 제외할 수 있다`() {
            transaction {
                val nick = uniq("중복유저")
                val handle = uniq("dup-h")
                val saved =
                    repository.saveWithSocial(
                        User.createWithSocial(
                            nickname = nick,
                            profileImageUrl = null,
                            socialProvider = SocialProvider.KAKAO,
                            socialId = uniq("sid-"),
                            handle = handle,
                        ),
                    )

                assertThat(repository.existsByNickname(nick)).isTrue()
                assertThat(repository.existsByNickname(uniq("안쓴닉"))).isFalse()
                assertThat(repository.existsByHandle(handle)).isTrue()
                assertThat(repository.existsByHandle(uniq("안쓴핸들"))).isFalse()

                // 자기 자신을 제외하면 자신이 점유한 값도 사용 가능으로 본다
                assertThat(repository.existsByNicknameExcludingUser(nick, saved.id.value)).isFalse()
                assertThat(repository.existsByHandleExcludingUser(handle, saved.id.value)).isFalse()
                // 다른 사용자 기준이면 여전히 점유 중
                assertThat(repository.existsByNicknameExcludingUser(nick, saved.id.value + 1)).isTrue()
                rollback()
            }
        }

        @Test
        fun `소프트 삭제하면 활성 조회에서 사라지고 탈퇴 소셜로만 찾을 수 있으며 재활성화로 되살아난다`() {
            transaction {
                val socialId = uniq("sid-")
                val saved =
                    repository.saveWithSocial(
                        User.createWithSocial(
                            nickname = uniq("탈퇴유저"),
                            profileImageUrl = null,
                            socialProvider = SocialProvider.APPLE,
                            socialId = socialId,
                            handle = uniq("wh"),
                        ),
                    )
                val id = saved.id.value

                val withdrawn =
                    User.reconstitute(
                        id = id,
                        nickname = saved.nickname,
                        socialProvider = SocialProvider.APPLE,
                        socialId = socialId,
                        status = UserStatus.WITHDRAWN,
                    )
                repository.softDelete(withdrawn)

                // 활성 조회에서 제외, 소셜 활성 조회도 제외
                assertThat(repository.findById(id)).isNull()
                assertThat(repository.findBySocial(SocialProvider.APPLE, socialId)).isNull()
                // 탈퇴 소셜 조회로는 잡힌다(재가입 판별용)
                assertThat(repository.findWithdrawnBySocial(SocialProvider.APPLE, socialId)?.id?.value).isEqualTo(id)

                val newHandle = uniq("newh")
                val newNick = uniq("재가입닉")
                val reactivating =
                    User
                        .reconstitute(
                            id = id,
                            nickname = newNick,
                            socialProvider = SocialProvider.APPLE,
                            socialId = socialId,
                            status = UserStatus.WITHDRAWN,
                        ).reactivate(nickname = newNick, handle = newHandle, profileImageUrl = null)

                val reactivated = repository.reactivate(reactivating)
                assertThat(reactivated.status).isEqualTo(UserStatus.ACTIVE)
                assertThat(reactivated.deletedAt).isNull()
                assertThat(reactivated.handle).isEqualTo(newHandle)
                assertThat(repository.findById(id)?.nickname).isEqualTo(newNick)
                rollback()
            }
        }

        @Test
        fun `findProfiles 와 findSummariesByIds 는 없거나 빈 입력을 견디고 활성만 반환한다`() {
            transaction {
                val a = repository.save(User.create(nickname = uniq("프로필A")))
                val b = repository.save(User.create(nickname = uniq("프로필B")))

                assertThat(repository.findProfiles(emptyList())).isEmpty()
                val profiles = repository.findProfiles(listOf(a.id.value, b.id.value, -999L))
                assertThat(profiles.map { it.id }).containsExactlyInAnyOrder(a.id.value, b.id.value)

                val summaries = repository.findSummariesByIds(listOf(a.id.value, -999L))
                assertThat(summaries.map { it.id }).containsExactly(a.id.value)
                assertThat(summaries.single().nickname).isEqualTo(a.nickname)

                // lockActive: 활성 id 만 잠금 후보로 되돌린다
                assertThat(repository.lockActive(emptyList())).isEmpty()
                assertThat(repository.lockActive(listOf(a.id.value, b.id.value)))
                    .containsExactlyInAnyOrder(a.id.value, b.id.value)
                rollback()
            }
        }

        @Test
        fun `update 는 활성 사용자의 프로필과 핸들을 갱신한다`() {
            transaction {
                val saved = repository.save(User.create(nickname = uniq("갱신전")))
                val newNick = uniq("갱신후")
                val newHandle = uniq("uph")
                val updatedDomain =
                    User
                        .reconstitute(id = saved.id.value, nickname = saved.nickname)
                        .updateProfile(nickname = newNick, handle = newHandle, bio = "새 소개")

                repository.update(updatedDomain)

                val reloaded = repository.findById(saved.id.value)!!
                assertThat(reloaded.nickname).isEqualTo(newNick)
                assertThat(reloaded.handle).isEqualTo(newHandle)
                assertThat(reloaded.bio).isEqualTo("새 소개")
                assertThat(repository.findByHandle(newHandle)?.id?.value).isEqualTo(saved.id.value)
                rollback()
            }
        }
    }
