package com.example.backend.bootstrap.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
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
