package com.example.backend.user.domain.model

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

/**
 * Identity 도메인 단위 테스트 — Spring 컨텍스트/DB 없이 순수하게 팩토리를 검증한다.
 */
class IdentityTest {
    @Test
    fun `create 는 자격증명 하나로 id 없이 새 Identity 를 생성한다`() {
        val credential = OAuthCredential.create(SocialProvider.KAKAO, "kakao-123")

        val identity = Identity.create(credential)

        assertThat(identity.id).isNull()
        assertThat(identity.credentials).containsExactly(credential)
    }
}
