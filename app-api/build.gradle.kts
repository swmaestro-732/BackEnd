plugins {
    id("backend.app-conventions")
    `jacoco-report-aggregation`
}

description = "Backend API 서버"

dependencies {
    implementation(project(":common"))
    implementation(project(":domain-rds"))
    implementation(project(":domain-search"))
    implementation(project(":core-web"))
    implementation(project(":client-tmap"))
    implementation(project(":client-kakao"))
    implementation(project(":client-oauth"))
    implementation(project(":client-s3"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-actuator") // health + /actuator/prometheus
    // 관측 — 메트릭(Prometheus 스크레이프) + 분산 트레이싱(OTLP → Tempo). 버전은 Boot BOM 관리.
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    // Spring Boot 4: OTLP 트레이스 리포팅 공식 스타터(Micrometer Tracing + OTel autoconfig + OTLP exporter 일괄).
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    // Sentry 에러 트래킹. Boot 4 전용 스타터(spring-boot-starter:4.1.0 대상). SENTRY_DSN 비면 no-op.
    implementation(libs.sentry.spring.boot)
    // OpenAPI 명세 + Swagger UI (springdoc 3.x = Spring Boot 4 호환)
    implementation(libs.springdoc.openapi)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    // 로깅 파사드 — SLF4J 위 얇은 래퍼. 코틀린 람다(지연) 로깅 `log.info { "$var" }`. logback/MDC/traceId 그대로.
    implementation(libs.kotlin.logging)
    // mobile BFF 서비스의 @Transactional(읽기 일관성).
    implementation("org.springframework:spring-tx")
    // GlobalExceptionHandler 가 DB UNIQUE 위반(ExposedSQLException)을 409 로 변환한다.
    implementation(libs.exposed.core)
    // 리인덱스 러너가 OpenSearchClient 빈 존재 여부로 동작을 정한다.
    implementation(libs.opensearch.java) {
        exclude(group = "org.opensearch.client", module = "opensearch-rest-client")
    }
    // SQS — 코스 개수 폴백 큐(course 이벤트 동기 반영 실패 시 재시도). spring-cloud-aws 4.0.x = Spring Boot 4 + Jackson 3.
    // @SqsListener(수신)·SqsTemplate(발행)을 자동 배선한다. 큐 미설정 시 리스너 비활성·발행 no-op(fail-soft).
    implementation(platform(libs.spring.cloud.aws.bom))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // 로컬 bootRun 시 docker-compose.yml 자동 기동 + DataSource 자동 연결. (운영 빌드엔 미포함)
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    // 통합 테스트가 테이블·엔티티로 픽스처를 직접 넣는다.
    testImplementation(libs.exposed.jdbc)
    testImplementation(libs.exposed.dao)
    testImplementation(libs.exposed.kotlin.datetime)
    testImplementation(libs.jackson2.module.kotlin)
    testImplementation(platform(libs.aws.sdk.bom))
    testImplementation("software.amazon.awssdk:s3")
    // 아키텍처 경계(헥사고날·도메인 분리)를 테스트로 강제
    testImplementation(libs.archunit)
    // OpenSearch 실연결 통합테스트용(태그드 opensearchIt 태스크에서만 사용) — 실제 OpenSearch 컨테이너 기동.
    // Spring Boot BOM 이 testcontainers 버전을 관리하지 않아 명시적으로 핀한다.
    testImplementation(libs.testcontainers.junit)
}

// 로컬 bootRun 이 루트의 docker-compose.yml 을 찾도록 작업 디렉터리를 루트로 둔다.
tasks.bootRun {
    workingDir = rootDir
}

tasks.withType<Test> {
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
        excludeTestsMatching("*OpenSearch*IntegrationTest")
    }
}

// OpenSearch 실연결 통합테스트(Testcontainers 로 실제 OpenSearch 기동). build/check 에 연결하지 않아
// 일반 test·PR CI 에서는 실행되지 않고, 로컬(`./gradlew opensearchIt`)·전용 워크플로에서만 돈다.
tasks.register<Test>("opensearchIt") {
    description = "OpenSearch 실연결 통합테스트(Testcontainers)"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("*OpenSearch*IntegrationTest") }
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

// 커버리지 리포트 — app-api 가 의존하는 모든 모듈의 test 결과를 하나로 합친다(CI 커버리지 게이트 입력).
// `./gradlew testCodeCoverageReport` → build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml
tasks.named<JacocoReport>("testCodeCoverageReport") {
    reports {
        xml.required.set(true) // CI 에서 커버리지 % 파싱용
        html.required.set(true)
    }
}
