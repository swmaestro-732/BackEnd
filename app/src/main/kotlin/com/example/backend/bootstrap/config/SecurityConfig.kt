package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
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
@EnableMethodSecurity
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
                    // 업로드 프리사인·`/api/v1/my/**`(저장함 등)·`/service/v1/my/**`(개인 화면 조합 BFF)은 경로 기준 JWT 인증 필수.
                    // 저장 장소(`/api/v1/saved-places`)·코스 저장(`/api/v1/saved-courses`)·저장 폴더(`/api/v1/folders`)는
                    // `/api/v1/my` 밖으로 옮긴 개인 리소스라 여기에 따로 적는다 —
                    // 안 적으면 아래 `/api` 하위 permitAll 에 걸려 무인증으로 열린다.
                    // user 도메인 계정 엔드포인트(`/api/v1/users`)는 경로가 아니라 @AccessTokenRequired 메서드 시큐리티로 보호한다.
                    .requestMatchers(
                        "/api/v1/my/**",
                        "/api/v1/saved-places",
                        "/api/v1/saved-places/**",
                        "/api/v1/saved-courses",
                        "/api/v1/saved-courses/**",
                        "/api/v1/folders",
                        "/api/v1/folders/**",
                        "/api/v1/uploads/**",
                        "/service/v1/my/**",
                    ).access { authentication, _ ->
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
                        // Prometheus 메트릭 — 스크레이프 소스는 SG로 모니터링 호스트만 허용(네트워크 격리).
                        "/actuator/prometheus",
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
