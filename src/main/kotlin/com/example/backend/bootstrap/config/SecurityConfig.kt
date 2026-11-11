package com.example.backend.bootstrap.config

import com.example.backend.admin.application.port.inbound.AdminAccessUseCase
import com.example.backend.bootstrap.security.AdminJwtTokenIssuer
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

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
    fun filterChain(
        http: HttpSecurity,
        adminAccessUseCase: AdminAccessUseCase,
    ): SecurityFilterChain {
        http
            .csrf { it.disable() } // 토큰 기반 무상태 API
            // 어드민 웹(정적 사이트)이 브라우저에서 /admin/v1/** 를 직접 부른다. 다른 경로는 CORS 를 열지 않는다.
            .cors { }
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
                    }
                    // 어드민 토큰(purpose=admin)은 앱 토큰과 별개다. 로그인만 공개하고 나머지는 어드민 토큰만 통과한다.
                    // 앱 access token 은 여기서, 어드민 토큰은 위 앱 경로(purpose=access 검사)에서 각각 거부된다.
                    // `/api/**` permitAll 보다 앞에 둬야 하고, 안 적으면 아래 anyRequest 에 걸려 앱 토큰도 통과한다.
                    .requestMatchers("/admin/v1/auth/login")
                    .permitAll()
                    .requestMatchers("/admin/v1/**")
                    // 토큰 서명만 믿지 않고 매 요청 admins 를 다시 조회한다. 행을 지우거나 등록 계정(google_sub)을 바꾸면
                    // 남은 토큰도 즉시 403 이 된다.
                    .access { authentication, _ ->
                        val resolved = authentication.get()
                        val token = (resolved as? JwtAuthenticationToken)?.token
                        val email = token?.subject
                        val googleSub = token?.getClaimAsString(AdminJwtTokenIssuer.GOOGLE_SUB_CLAIM)
                        AuthorizationDecision(
                            token?.getClaimAsString("purpose") == "admin" &&
                                email != null &&
                                googleSub != null &&
                                adminAccessUseCase.isAdmin(email, googleSub),
                        )
                    }.requestMatchers(
                        "/actuator/health",
                        "/actuator/health/liveness",
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

    /**
     * 어드민 API(/admin/v1 하위)에만 어드민 웹 오리진의 CORS 를 허용한다(preflight 는 인가 전에 응답).
     * 토큰은 Authorization 헤더로만 오고 쿠키를 쓰지 않으므로 credentials 는 허용하지 않는다.
     */
    @Bean
    fun corsConfigurationSource(
        @Value("\${admin.web-origins}") adminWebOrigins: List<String>,
    ): CorsConfigurationSource =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/admin/v1/**",
                CorsConfiguration().apply {
                    allowedOrigins = adminWebOrigins
                    allowedMethods = listOf("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                    allowedHeaders = listOf("Authorization", "Content-Type")
                    allowCredentials = false
                    maxAge = 3600
                },
            )
        }
}
