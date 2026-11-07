// 내부 계층(core-web): 웹, 보안 공통. 보안 필터 체인, 웹 애노테이션과 인자 리졸버, 전역 예외 핸들러, OpenAPI, Sentry, 목 아스펙트.
// 원칙: 도메인과 영속성(Exposed)을 모른다. 토큰 검증용 JwtDecoder 빈과 DB 예외 변환은 app-api 가 가진다.
plugins {
    id("backend.kotlin-conventions")
}

dependencies {
    // Spring Modulith 모듈 선언 애노테이션(package-info.java). 컴파일에만 쓰고 런타임 의존은 늘리지 않는다
    compileOnly(platform(libs.spring.modulith.bom))
    compileOnly("org.springframework.modulith:spring-modulith-api")
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation(libs.springdoc.openapi.webmvc.ui)
    implementation(libs.sentry.spring.boot)
    implementation(libs.kotlin.logging)
}
