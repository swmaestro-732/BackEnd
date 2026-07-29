package com.example.backend.user.adapter.inbound.web

import com.example.backend.support.IntegrationTestBase
import com.example.backend.user.application.port.outbound.UserPersistencePort
import com.example.backend.user.domain.model.SocialProvider
import com.example.backend.user.domain.model.UserStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.transaction.annotation.Transactional

/**
 * 탈퇴(soft delete)한 소셜 계정의 재로그인 → 재활성화가 **기존 행을 재사용**하는지(INSERT 아님) 검증한다.
 * 새 INSERT 면 uq_users_social UNIQUE 위반으로 500 이 난다. 실제 소셜 로그인 흐름은 Kakao 검증이 필요해
 * 통합에서 구동 불가하므로, AuthService 가 부르는 영속성 계약(재활성화)을 어댑터 레벨에서 직접 검증한다.
 * (재활성화 전이 로직 자체는 AuthServiceTest 단위 테스트가 담당.)
 */
@Sql(
    statements =
        [
            "TRUNCATE TABLE users RESTART IDENTITY CASCADE",
            "INSERT INTO users (nickname, handle, status, social_provider, social_id, deleted_at) " +
                "VALUES ('탈퇴유저', 'old_handle', 3, 'KAKAO', 'withdrawn-social-id', now())",
        ],
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
class AuthReactivationFlowTest
    @Autowired
    constructor(
        private val userPersistencePort: UserPersistencePort,
    ) : IntegrationTestBase() {
        @Test
        @Transactional
        fun `탈퇴 소셜 계정 재활성화는 기존 행을 재사용한다(온보딩 리셋)`() {
            val withdrawn = userPersistencePort.findWithdrawnBySocial(SocialProvider.KAKAO, SOCIAL_ID)
            assertNotNull(withdrawn)

            val reactivated = userPersistencePort.reactivate(withdrawn!!.reactivate())

            // 같은 id 재사용 — 새 INSERT 가 아니라 기존 행 UPDATE(uq_users_social 위반 없음).
            assertEquals(WITHDRAWN_USER_ID, reactivated.id)
            assertEquals(UserStatus.ACTIVE, reactivated.status)
            // 재활성화는 온보딩 리셋 — 닉네임·핸들이 비워진다.
            assertNull(reactivated.nickname)
            assertNull(reactivated.handle)

            // 이제 활성 계정으로 조회된다(같은 행).
            val active = userPersistencePort.findBySocial(SocialProvider.KAKAO, SOCIAL_ID)
            assertEquals(WITHDRAWN_USER_ID, active?.id)
        }

        private companion object {
            const val WITHDRAWN_USER_ID = 1L
            const val SOCIAL_ID = "withdrawn-social-id"
        }
    }
