package com.example.backend.bootstrap.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtConfig(
    private val jwtProperties: JwtProperties,
    private val kakaoOauthProperties: KakaoOauthProperties,
) {
    private val secretKey = SecretKeySpec(jwtProperties.secret.toByteArray(), "HmacSHA256")

    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey))

    @Bean
    @Primary
    fun jwtDecoder(): JwtDecoder =
        NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()

    @Bean
    @Qualifier("kakaoJwtDecoder")
    fun kakaoJwtDecoder(): JwtDecoder {
        val decoder =
            NimbusJwtDecoder
                .withJwkSetUri(kakaoOauthProperties.jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build()

        val issuerValidator = JwtValidators.createDefaultWithIssuer(kakaoOauthProperties.issuer)
        // aud 는 로그인 방식마다 다르다 — 웹=REST API 키(clientId), 안드로이드/iOS SDK=네이티브 앱 키.
        // 설정된 키(빈 값 제외) 중 하나라도 aud 에 있으면 통과한다.
        val allowedAudiences =
            listOf(kakaoOauthProperties.clientId, kakaoOauthProperties.nativeAppKey)
                .filter { it.isNotBlank() }
                .toSet()
        val validator =
            DelegatingOAuth2TokenValidator(
                issuerValidator,
                audienceValidator(allowedAudiences),
            )
        decoder.setJwtValidator(validator)
        return decoder
    }

    private fun audienceValidator(allowedAudiences: Set<String>): OAuth2TokenValidator<Jwt> =
        OAuth2TokenValidator { jwt ->
            if (jwt.audience.orEmpty().any { it in allowedAudiences }) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error(
                        "invalid_token",
                        "Kakao ID token audience does not match a configured Kakao app key.",
                        null,
                    ),
                )
            }
        }
}
