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
    private val googleOauthProperties: GoogleOauthProperties,
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

    @Bean
    @Qualifier("googleJwtDecoder")
    fun googleJwtDecoder(): JwtDecoder {
        val decoder =
            NimbusJwtDecoder
                .withJwkSetUri(googleOauthProperties.jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build()

        // Google 은 iss 를 "https://accounts.google.com" 또는 "accounts.google.com" 두 형태로 발급한다 — 둘 다 허용.
        val allowedIssuers =
            setOf(
                googleOauthProperties.issuer,
                googleOauthProperties.issuer.removePrefix("https://"),
            )
        // aud 는 플랫폼(web/android/ios)마다 다른 client-id. 설정된 값(빈 값 제외) 중 하나라도 있으면 통과.
        val allowedAudiences =
            listOf(
                googleOauthProperties.clientId,
                googleOauthProperties.androidClientId,
                googleOauthProperties.iosClientId,
            ).filter { it.isNotBlank() }.toSet()
        // createDefault() 를 반드시 포함한다 — setJwtValidator 는 기본 검증기를 통째로 대체하므로
        // 빠뜨리면 exp(만료)·nbf 검증이 사라진다(만료된 토큰 통과). issuer/aud 는 그 위에 얹는다.
        val validator =
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefault(),
                issuerValidator(allowedIssuers),
                audienceValidator(allowedAudiences),
            )
        decoder.setJwtValidator(validator)
        return decoder
    }

    private fun issuerValidator(allowedIssuers: Set<String>): OAuth2TokenValidator<Jwt> =
        OAuth2TokenValidator { jwt ->
            if (jwt.issuer?.toString() in allowedIssuers) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(
                    OAuth2Error("invalid_token", "ID token issuer is not an allowed issuer.", null),
                )
            }
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
