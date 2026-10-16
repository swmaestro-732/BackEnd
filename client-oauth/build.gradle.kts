plugins {
    id("backend.library-conventions")
}

// 소셜 로그인 토큰 검증 수단 — 카카오·구글·애플 ID 토큰 디코더, 네이버 프로필 API 클라이언트.
dependencies {
    api("org.springframework.security:spring-security-oauth2-jose")

    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework:spring-web")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation(libs.kotlin.logging)
}
