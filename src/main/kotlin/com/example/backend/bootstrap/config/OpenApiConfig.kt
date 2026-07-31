package com.example.backend.bootstrap.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI(Swagger) 문서 메타데이터 + JWT(Bearer) 인증 스키마.
 * Swagger UI: /swagger-ui.html · OpenAPI JSON: /v3/api-docs
 * Swagger UI 의 Authorize 버튼에 JWT 를 넣으면 요청에 `Authorization: Bearer <token>` 가 실린다.
 * 실제 토큰 검증은 SecurityConfig 의 OAuth2 Resource Server 가 처리한다.
 */
@Configuration
class OpenApiConfig {
    private val bearerScheme = "bearerAuth"

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            // CloudFront(https) → ALB/EC2(http) 뒤라 요청 스킴이 http 로 보여 Swagger 가 http 서버 URL 을
            // 만들고 "Try it out" 이 깨진다. 상대 경로("/")로 두면 Swagger UI 가 문서를 연 브라우저
            // origin(운영=https://api.courmy.com, 로컬=http://localhost)에 맞춰 호출한다.
            .servers(listOf(Server().url("/")))
            .info(
                Info()
                    .title("칠삼이 Backend API")
                    .description("칠삼이 백엔드 REST API 문서")
                    .version("v0.0.1"),
            ).components(
                Components().addSecuritySchemes(
                    bearerScheme,
                    SecurityScheme()
                        .name(bearerScheme)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )
            // 전역 적용: 모든 엔드포인트에 자물쇠 표시 + Authorize 토큰 전송
            .addSecurityItem(SecurityRequirement().addList(bearerScheme))
}
