package com.example.backend.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI(Swagger) 문서 메타데이터.
 * Swagger UI: /swagger-ui.html · OpenAPI JSON: /v3/api-docs
 */
@Configuration
class OpenApiConfig {
    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("칠삼이 Backend API")
                .description("칠삼이 백엔드 REST API 문서")
                .version("v0.0.1"),
        )
}
