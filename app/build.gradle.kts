plugins {
    id("backend.kotlin-conventions")
    id("org.springframework.boot")
}

description = "Backend 실행 모듈 — Spring Boot 부트스트랩·설정·리소스(application.yml·Flyway)·bootJar 산출물"

dependencies {
    implementation(project(":common"))
    implementation(project(":area"))
    implementation(project(":direction"))
    implementation(project(":media"))
    implementation(project(":place"))
    implementation(project(":user-api"))
    implementation(project(":course"))
    implementation(project(":user"))
    implementation(project(":mobile"))

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

    // Sentry 에러 트래킹. Boot 4 전용 스타터(spring-boot-starter:4.1.0 대상). SENTRY_DSN 비면 no-op.
    implementation("io.sentry:sentry-spring-boot-4-starter:8.53.0")
    // OpenAPI 명세 + Swagger UI (springdoc 3.x = Spring Boot 4 호환)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    // opensearch-java 의 JacksonJsonpMapper 는 Jackson 2(com.fasterxml)를 쓴다 — 검색 응답을 Kotlin data class
    // (CourseDocument)로 역직렬화하려면 Jackson 2 용 kotlin 모듈이 필요하다(색인=직렬화는 없이도 되지만 읽기는 불가).
    // 버전은 opensearch-java 2.25.0 이 끌어오는 jackson-databind 2.21.4 에 맞춘다.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.4")
    // 로깅 파사드 — SLF4J 위 얇은 래퍼. 코틀린 람다(지연) 로깅 `log.info { "$var" }`. logback/MDC/traceId 그대로.
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // 로컬 bootRun 시 docker-compose.yml 자동 기동 + DataSource 자동 연결. (운영 빌드엔 미포함)
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    implementation("org.postgresql:postgresql")
    // AWS SDK v2 (raw) — S3Presigner 는 s3 모듈 소속. 보안 패치용 버전은 buildSrc 컨벤션 플러그인의 extra[] 가 관리한다.
    implementation(platform("software.amazon.awssdk:bom:2.49.0"))
    implementation("software.amazon.awssdk:s3")
    // OpenSearch IAM/SigV4 전송에 필요한 동기 SdkHttpClient. 버전은 위 AWS SDK BOM 관리.
    implementation("software.amazon.awssdk:apache-client")
    // SQS — 코스 개수 폴백 큐(course 이벤트 동기 반영 실패 시 재시도). spring-cloud-aws 4.0.x = Spring Boot 4 + Jackson 3.
    // @SqsListener(수신)·SqsTemplate(발행)을 자동 배선한다. 큐 미설정 시 리스너 비활성·발행 no-op(fail-soft).
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:4.0.2"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    // OpenSearch(AWS) 연결 — 기본 FGAC basic auth(ApacheHttpClient5), 선택 IAM/SigV4(AwsSdk2Transport).
    // 사용하지 않는 구형 RestClient 전송(opensearch-rest-client)은 제외한다.
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

// app 은 실행 전용 — 라이브러리용 plain jar 는 만들지 않는다(Docker COPY *.jar 단일 매칭 보장).
tasks.jar {
    enabled = false
}

// 멀티모듈 전환으로 bootRun 작업 디렉터리가 app/ 이 됐다. spring-boot-docker-compose 가
// 루트의 docker-compose.yml 을 찾도록 단일 모듈 시절과 같은 루트로 고정한다.
tasks.bootRun {
    workingDir = rootProject.projectDir
}

// 메인 test: 아키텍처(ArchUnit) 테스트와 OpenSearch 실연결 통합테스트는 제외 —
// 각각 전용 archTest·opensearchIt 에서만 실행(PR CI 를 무겁게 하지 않음).
tasks.test {
    filter {
        excludeTestsMatching("com.example.backend.architecture.*")
        excludeTestsMatching("*OpenSearch*IntegrationTest")
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

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true) // CI 에서 커버리지 % 파싱용
        html.required.set(true)
    }
}
