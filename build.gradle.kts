plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Exposed 핵심 모듈
    implementation("org.jetbrains.exposed:exposed-jdbc:1.3.0")
    implementation("org.jetbrains.exposed:exposed-core:1.3.0")
    implementation("org.jetbrains.exposed:exposed-dao:1.3.0")
    // 날짜/시간 지원
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:1.3.0")

    // Exposed ↔ Spring 연동 (Database 자동구성 + @Transactional 지원)
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:1.3.0")

    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jdbc") // HikariCP 포함
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator") // health
    // OpenAPI 명세 + Swagger UI (springdoc 3.x = Spring Boot 4 호환)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // 로컬 bootRun 시 docker-compose.yml 자동 기동 + DataSource 자동 연결. (운영 빌드엔 미포함)
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    implementation("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    // 아키텍처 경계(헥사고날·도메인 분리)를 테스트로 강제
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 메인 test: 아키텍처(ArchUnit) 테스트는 제외 — 전용 archTest 에서만 실행(이중 실행 방지).
tasks.test {
    filter { excludeTestsMatching("com.example.backend.architecture.*") }
    finalizedBy(tasks.jacocoTestReport) // 테스트 후 커버리지 리포트 생성
}

// ArchUnit 아키텍처 경계 규칙만 실행하는 전용 태스크(DB 불필요). CI 의 architecture 잡이 사용.
// build/check 에 연결하지 않아 build & test 에서는 실행되지 않는다.
tasks.register<Test>("archTest") {
    description = "ArchUnit 아키텍처 경계 규칙만 실행"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("com.example.backend.architecture.*") }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true) // CI 에서 커버리지 % 파싱용
        html.required.set(true)
    }
}
