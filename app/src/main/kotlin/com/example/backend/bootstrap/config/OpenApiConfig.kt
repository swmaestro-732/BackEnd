package com.example.backend.bootstrap.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
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

    /**
     * Swagger UI 상단 그룹을 **도메인 패키지 기준**으로 묶는다(컨트롤러 클래스 단위 태그 대신).
     * 각 그룹은 `com.example.backend.<domain>` 하위 컨트롤러만 스캔한다.
     * mobile 은 화면 조합(BFF, /service/v1) 전용 패키지라 별도 그룹으로 둔다.
     */
    private fun group(
        name: String,
        vararg packages: String,
    ): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group(name)
            .packagesToScan(*packages)
            .build()

    @Bean
    fun userApi(): GroupedOpenApi = group("user", "com.example.backend.user")

    @Bean
    fun areaApi(): GroupedOpenApi = group("area", "com.example.backend.area")

    @Bean
    fun courseApi(): GroupedOpenApi = group("course", "com.example.backend.course")

    @Bean
    fun placeApi(): GroupedOpenApi = group("place", "com.example.backend.place")

    @Bean
    fun mediaApi(): GroupedOpenApi = group("media", "com.example.backend.media")

    @Bean
    fun directionApi(): GroupedOpenApi = group("direction", "com.example.backend.direction")

    @Bean
    fun mobileApi(): GroupedOpenApi = group("mobile", "com.example.backend.mobile")
}
