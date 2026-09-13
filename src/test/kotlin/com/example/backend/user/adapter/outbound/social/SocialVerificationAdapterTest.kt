package com.example.backend.user.adapter.outbound.social

import com.example.backend.bootstrap.security.KakaoOauthProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import com.example.backend.user.domain.model.SocialProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException

class SocialVerificationAdapterTest {
    private val decoder = mock(JwtDecoder::class.java)
    private val props =
        KakaoOauthProperties(
            clientId = "test-client-id",
            jwksUri = "https://kauth.kakao.com/.well-known/jwks.json",
            issuer = "https://kauth.kakao.com",
        )
    private val adapter = SocialVerificationAdapter(decoder, props)

    private fun validJwt(subject: String): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "RS256")
            .subject(subject)
            .build()

    @Test
    fun `유효한 KAKAO idToken 이면 SocialIdentity 를 반환한다`() {
        `when`(decoder.decode("valid-token")).thenReturn(validJwt("kakao-user-123"))

        val identity = adapter.verify(SocialProvider.KAKAO, "valid-token")

        assertEquals(SocialProvider.KAKAO, identity.provider)
        assertEquals("kakao-user-123", identity.socialId)
    }

    @Test
    fun `clientId 가 비어 있으면 SOCIAL_AUTHENTICATION_FAILED 예외를 던진다`() {
        val noClientAdapter =
            SocialVerificationAdapter(
                decoder,
                KakaoOauthProperties(
                    clientId = "",
                    jwksUri = "https://kauth.kakao.com/.well-known/jwks.json",
                    issuer = "https://kauth.kakao.com",
                ),
            )

        val ex = assertThrows<BusinessException> { noClientAdapter.verify(SocialProvider.KAKAO, "token") }
        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, ex.errorCode)
    }

    @Test
    fun `KAKAO 외 provider 는 SOCIAL_AUTHENTICATION_FAILED 예외를 던진다`() {
        val ex = assertThrows<BusinessException> { adapter.verify(SocialProvider.APPLE, "token") }
        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, ex.errorCode)
    }

    @Test
    fun `JwtDecoder 가 JwtException 을 던지면 SOCIAL_AUTHENTICATION_FAILED 예외로 변환한다`() {
        `when`(decoder.decode("bad-token")).thenThrow(JwtException("invalid"))

        val ex = assertThrows<BusinessException> { adapter.verify(SocialProvider.KAKAO, "bad-token") }
        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, ex.errorCode)
    }

    @Test
    fun `JWT subject 가 null 이면 SOCIAL_AUTHENTICATION_FAILED 예외를 던진다`() {
        val jwt =
            Jwt
                .withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", "https://kauth.kakao.com")
                .build()
        `when`(decoder.decode("no-sub")).thenReturn(jwt)

        val ex = assertThrows<BusinessException> { adapter.verify(SocialProvider.KAKAO, "no-sub") }
        assertEquals(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED, ex.errorCode)
    }
}
