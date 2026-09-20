package com.example.backend.user.adapter.outbound.social

import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.application.port.outbound.SocialIdentity
import com.example.backend.user.domain.model.SocialProvider
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SocialVerificationAdapterTest {
    @Test
    fun `provider에 맞는 검증기로 위임한다`() {
        val adapter = SocialVerificationAdapter(listOf(fake(SocialProvider.KAKAO), fake(SocialProvider.GOOGLE)))

        val kakao = adapter.verify(SocialProvider.KAKAO, "token")
        val google = adapter.verify(SocialProvider.GOOGLE, "token")

        assertEquals(SocialProvider.KAKAO, kakao.provider)
        assertEquals("KAKAO-token", kakao.socialId)
        assertEquals(SocialProvider.GOOGLE, google.provider)
        assertEquals("GOOGLE-token", google.socialId)
    }

    @Test
    fun `등록되지 않은 provider는 인증 실패로 거부한다`() {
        val adapter = SocialVerificationAdapter(listOf(fake(SocialProvider.KAKAO)))

        val exception = assertFailsWith<BusinessException> { adapter.verify(SocialProvider.GOOGLE, "token") }

        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, exception.errorCode)
    }

    private fun fake(forProvider: SocialProvider): SocialTokenVerifier =
        object : SocialTokenVerifier {
            override val provider: SocialProvider = forProvider

            override fun verify(idToken: String): SocialIdentity =
                SocialIdentity(provider = forProvider, socialId = "${forProvider.name}-$idToken")
        }
}
