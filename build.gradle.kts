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
    implementation("org.springframework.boot:spring-boot-starter-actuator") // health + /actuator/prometheus
    implementation("org.springframework.boot:spring-boot-starter-aspectj") // AOP (Spring Boot 4에서 starter-aop 대체)
    // 관측 — 메트릭(Prometheus 스크레이프) + 분산 트레이싱(OTLP → Tempo). 버전은 Boot BOM 관리.
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    // Spring Boot 4: OTLP 트레이스 리포팅 공식 스타터(Micrometer Tracing + OTel autoconfig + OTLP exporter 일괄).
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    // OpenAPI 명세 + Swagger UI (springdoc 3.x = Spring Boot 4 호환)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // 로컬 bootRun 시 docker-compose.yml 자동 기동 + DataSource 자동 연결. (운영 빌드엔 미포함)
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    implementation("org.postgresql:postgresql")
    // AWS SDK v2 (raw — spring-cloud-aws 는 아직 Boot 4.x 지원이 뒤처져 있음). S3Presigner 는 s3 모듈 소속.
    implementation(platform("software.amazon.awssdk:bom:2.49.0"))
    implementation("software.amazon.awssdk:s3")
    // OpenSearch(AWS) 연결 — VPC 도메인에 HTTPS + FGAC basic auth. ApacheHttpClient5 전송.
    // httpclient5 기반 ApacheHttpClient5 전송만 쓰므로, 구형 RestClient 전송(opensearch-rest-client)이
    // 끌고 오는 httpclient 4.x 스택(httpclient/httpcore/httpasyncclient)은 제외한다 — 안 쓰는 중복 무게 제거.
    implementation("org.opensearch.client:opensearch-java:2.25.0") {
        exclude(group = "org.opensearch.client", module = "opensearch-rest-client")
    }
    implementation("org.apache.httpcomponents.client5:httpclient5")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    // 아키텍처 경계(헥사고날·도메인 분리)를 테스트로 강제
    testImplementation("com.tngtech.archunit:archunit-junit5:1.3.0")

    // OpenSearch 실연결 통합테스트용(태그드 opensearchIt 태스크에서만 사용) — 실제 OpenSearch 컨테이너 기동.
    // Spring Boot BOM 이 testcontainers 버전을 관리하지 않아 명시적으로 핀한다.
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // 컨텍스트 부팅에 필요한 더미 env — test·opensearchIt 공통(archTest 는 컨텍스트 미부팅이라 무시됨).
    // JWT 시크릿은 코드 기본값 없이 env 주입 — 테스트는 전용 더미(운영용 아님).
    environment("JWT_SECRET", "test-only-jwt-secret-not-for-production-0123456789")
    // S3 프리사인은 네트워크 호출 없이 로컬 서명만 계산하지만, 버킷명은 비어 있으면 SDK가 거부한다.
    environment("S3_MEDIA_BUCKET", "test-media-bucket")
    environment("S3_MEDIA_CDN_URL", "https://cdn.test.example.com")
    // DefaultCredentialsProvider 가 EC2 인스턴스 메타데이터까지 폴백하며 네트워크를 타지 않도록 더미 고정 자격증명 주입.
    environment("AWS_ACCESS_KEY_ID", "test-access-key")
    environment("AWS_SECRET_ACCESS_KEY", "test-secret-key")
}

// 메인 test: 아키텍처(ArchUnit) 테스트와 OpenSearch 실연결 통합테스트는 제외 —
// 각각 전용 archTest·opensearchIt 에서만 실행(PR CI 를 무겁게 하지 않음).
tasks.test {
    filter {
        excludeTestsMatching("com.example.backend.architecture.*")
        excludeTestsMatching("com.example.backend.bootstrap.config.OpenSearchIntegrationTest")
    }
    finalizedBy(tasks.jacocoTestReport) // 테스트 후 커버리지 리포트 생성
}

// OpenSearch 실연결 통합테스트(Testcontainers 로 실제 OpenSearch 기동). build/check 에 연결하지 않아
// 일반 test·PR CI 에서는 실행되지 않고, 로컬(`./gradlew opensearchIt`)·전용 워크플로에서만 돈다.
tasks.register<Test>("opensearchIt") {
    description = "OpenSearch 실연결 통합테스트(Testcontainers)"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("com.example.backend.bootstrap.config.OpenSearchIntegrationTest") }
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
