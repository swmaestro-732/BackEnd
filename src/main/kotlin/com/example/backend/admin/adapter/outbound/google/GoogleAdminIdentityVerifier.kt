package com.example.backend.admin.adapter.outbound.google

import com.example.backend.admin.application.port.outbound.AdminIdentityPort
import com.example.backend.bootstrap.security.GoogleOauthProperties
import com.example.backend.common.exception.BusinessException
import com.example.backend.common.response.CommonErrorCode
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component

/**
 * 어드민 로그인용 구글 ID 토큰 검증. 서명/iss/exp 는 앱과 같은 googleJwtDecoder 가 본다.
 * 그 decoder 는 web/android/ios aud 를 모두 받으므로, 어드민(웹 콘솔)은 web client-id aud 만 추가로 허용한다.
 */
@Component
class GoogleAdminIdentityVerifier(
    @param:Qualifier("googleJwtDecoder")
    private val googleJwtDecoder: JwtDecoder,
    private val googleOauthProperties: GoogleOauthProperties,
) : AdminIdentityPort {
    override fun verifyGoogle(idToken: String): String {
        val webClientId = googleOauthProperties.clientId
        if (webClientId.isBlank()) fail()
        val jwt =
            try {
                googleJwtDecoder.decode(idToken)
            } catch (exception: JwtException) {
                throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
            }
        if (webClientId !in jwt.audience.orEmpty()) fail()
        // 구글이 소유를 확인한 이메일만 신원으로 쓴다. email_verified 는 Boolean 또는 문자열로 온다.
        val emailVerified = jwt.claims["email_verified"]
        if (emailVerified != true && emailVerified != "true") fail()
        val email = jwt.getClaimAsString("email")?.takeIf(String::isNotBlank) ?: fail()
        return email.lowercase()
    }

    private fun fail(): Nothing = throw BusinessException(CommonErrorCode.SOCIAL_AUTHENTICATION_FAILED)
}
