plugins {
    id("backend.library-conventions")
}

// 웹 공통 — 보안(JWT·인증 해석), 응답 엔벨로프, 모킹 AOP, OpenAPI. 비즈니스를 모른다.
dependencies {
    api(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation(libs.springdoc.openapi)
}
