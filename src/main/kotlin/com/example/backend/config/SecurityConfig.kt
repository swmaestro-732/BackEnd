package com.example.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * REST API 보안 기본 설정.
 * security 스타터가 켜져 있으면 기본적으로 모든 요청이 잠기므로(401),
 * 여기서 stateless + 공개 경로를 정의한다. 인증(JWT 등) 도입 시 이 파일에서 강화.
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
            .authorizeHttpRequests {
                it
                    .requestMatchers(
                        "/actuator/health",
                        "/api/**",
                        // Swagger UI / OpenAPI 문서 (springdoc)
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                    ).permitAll()
                    // 그 외 경로는 기본 차단(default deny) — 새 엔드포인트가 검토 없이 공개되지 않도록.
                    // 인증 도입 시 여기에 인증/인가 규칙을 추가한다.
                    .anyRequest()
                    .authenticated()
            }
        return http.build()
    }
}
