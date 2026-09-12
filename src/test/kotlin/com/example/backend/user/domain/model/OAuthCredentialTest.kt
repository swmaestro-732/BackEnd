package com.example.backend.user.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

/**
 * OAuthCredential 도메인 단위 테스트 — Spring 컨텍스트/DB 없이 순수하게 팩토리와 불변식을 검증한다.
 */
class OAuthCredentialTest {
    @Test
    fun `create 는 유효한 socialId 로 자격증명을 생성한다`() {
        val cred = OAuthCredential.create(SocialProvider.KAKAO, "kakao-123")

        assertThat(cred.provider).isEqualTo(SocialProvider.KAKAO)
        assertThat(cred.socialId).isEqualTo("kakao-123")
    }

    @Test
    fun `create 는 socialId 가 공백만이면 IllegalArgumentException 을 던진다`() {
        assertThrows<IllegalArgumentException> {
            OAuthCredential.create(SocialProvider.KAKAO, "   ")
        }
    }

    @Test
    fun `create 는 socialId 가 빈 문자열이면 IllegalArgumentException 을 던진다`() {
        assertThrows<IllegalArgumentException> {
            OAuthCredential.create(SocialProvider.KAKAO, "")
        }
    }
}
