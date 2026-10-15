// 어플리케이션 계층: 실행 가능한 API 서버. 하위 모듈(domain-rds, client-opensearch, common)을 조립해 서비스를 완성한다.
// 포함: main, 설정(보안, JWT, OpenAPI 등), 인바운드 어댑터(컨트롤러, SQS 소비), BFF(mobile),
//       RDB 외 아웃바운드 어댑터(OpenSearch 검색/색인, S3, SQS 발행, Kakao/TMAP/소셜 로그인), 색인 초기화/재색인, 전체 테스트.
// 유일하게 bootJar 를 만든다.
plugins {
    id("backend.kotlin-conventions")
    id("org.springframework.boot")
    jacoco
}

// 실행 가능한 bootJar 만 산출(라이브러리용 plain jar 는 끈다). build/libs 에 jar 가 하나만 남아 Docker COPY *.jar 가 모호하지 않다.
tasks.named<Jar>("jar") { enabled = false }

dependencies {
    implementation(project(":common"))
    implementation(project(":domain-rds"))
    implementation(project(":client-opensearch"))

    // 웹, 보안, 검증
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation(libs.springdoc.openapi.webmvc.ui)
    implementation("org.apache.httpcomponents.client5:httpclient5")

    // 직렬화, 로깅, 날짜
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation(libs.jackson.module.kotlin.v2)
    implementation(libs.kotlin.logging)
    implementation(libs.kotlinx.datetime)

    // RDB 자동구성과 마이그레이션 실행(스키마와 테이블 정의는 domain-rds 가 가진다)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.spring.boot.starter)
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")

    // AWS: S3(미디어 업로드 프리사인), SQS(코스 개수 폴백 큐)
    implementation(platform(libs.aws.bom))
    implementation(libs.aws.s3)
    implementation(platform(libs.spring.cloud.aws.bom))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")

    // 관측
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation(libs.sentry.spring.boot)

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.testcontainers.junit)
}

springBoot {
    mainClass.set("com.example.backend.BackendApplicationKt")
}

// 메인 test: ArchUnit, OpenSearch 실연결 통합테스트는 제외(각각 전용 태스크에서만).
tasks.test {
    filter {
        excludeTestsMatching("com.example.backend.architecture.*")
        excludeTestsMatching("*OpenSearch*IntegrationTest")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register<Test>("opensearchIt") {
    description = "OpenSearch 실연결 통합테스트(Testcontainers)"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("*OpenSearch*IntegrationTest") }
}

// ArchUnit 아키텍처 경계 규칙만 실행(DB 불필요). 도메인(패키지) 간 경계와 레이어 경계를 강제한다.
tasks.register<Test>("archTest") {
    description = "ArchUnit 아키텍처 경계 규칙만 실행"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("com.example.backend.architecture.*") }
}

// 테스트는 이 모듈에 있지만 코드는 하위 모듈에도 있다. 전 모듈을 합친 집계 리포트를 만든다.
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    val coveredModules = listOf("common", "domain-rds", "client-opensearch", "app-api")
    sourceDirectories.setFrom(files(coveredModules.map { rootProject.project(it).file("src/main/kotlin") }))
    classDirectories.setFrom(
        files(
            coveredModules.map {
                rootProject
                    .project(it)
                    .layout.buildDirectory
                    .dir("classes/kotlin/main")
            },
        ),
    )
    executionData.setFrom(fileTree(layout.buildDirectory).include("jacoco/test.exec"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
