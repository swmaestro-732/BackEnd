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
        val validator =
            if (kakaoOauthProperties.clientId.isBlank()) {
                issuerValidator
            } else {
                DelegatingOAuth2TokenValidator(
                    issuerValidator,
                    audienceValidator(kakaoOauthProperties.clientId),
                )
            }
        decoder.setJwtValidator(validator)
        return decoder
    }

    private fun audienceValidator(clientId: String): OAuth2TokenValidator<Jwt> =
        OAuth2TokenValidator { jwt ->
            if (clientId in jwt.audience.orEmpty()) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error(
                        "invalid_token",
                        "Kakao ID token audience does not contain the configured client ID.",
                        null,
                    ),
                )
            }
        }
}
