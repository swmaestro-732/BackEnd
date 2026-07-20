package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain

/**
 * REST API 보안 기본 설정.
 * security 스타터가 켜져 있으면 기본적으로 모든 요청이 잠기므로(401),
 * 여기서 stateless JWT 인증과 공개 경로를 정의한다.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // 토큰 기반 무상태 API
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .oauth2ResourceServer { it.jwt { } }
            .authorizeHttpRequests {
                it
                    // 첫 매치 우선: 현재 사용자("나") 기준 API 는 JWT 인증 필수.
                    .requestMatchers("/api/v1/my/**")
                    .access { authentication, _ ->
                        val resolved = authentication.get()
                        AuthorizationDecision(
                            resolved.isAuthenticated &&
                                resolved !is AnonymousAuthenticationToken &&
                                (
                                    resolved !is JwtAuthenticationToken ||
                                        resolved.token.getClaimAsString("purpose") == "access"
                                ),
                        )
                    }.requestMatchers(
                        "/actuator/health",
                        "/api/**",
                        // 화면 조합(BFF) 엔드포인트
                        "/service/**",
                        // Swagger UI / OpenAPI 문서 (springdoc)
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                    ).permitAll()
                    // 그 외 경로는 기본 차단(default deny) — 새 엔드포인트가 검토 없이 공개되지 않도록.
                    .anyRequest()
                    .authenticated()
            }
        return http.build()
    }
}
