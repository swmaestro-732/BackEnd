// :bootstrap — Spring Boot 애플리케이션(main) + config/security + resources + 전체 테스트.
// 모든 도메인 모듈에 의존하며 유일하게 실행 가능한 bootJar 를 만든다.
// 공유 인프라(web/security/Exposed/검색/AWS 등)는 루트 subprojects 에서 상속하고, 여기선 부트 전용(자동구성/관측/마이그레이션)만 더한다.
plugins {
    id("org.springframework.boot")
    jacoco
}

// 실행 가능한 bootJar 만 산출(라이브러리용 plain jar 는 끈다) — build/libs 에 jar 가 하나만 남게 해 Docker COPY *.jar 모호성 제거.
tasks.named<Jar>("jar") { enabled = false }

dependencies {
    implementation(project(":common"))
    implementation(project(":user-api"))
    implementation(project(":area"))
    implementation(project(":media"))
    implementation(project(":direction"))
    implementation(project(":place"))
    implementation(project(":course"))
    implementation(project(":user"))
    implementation(project(":mobile"))

    // 부트 전용(자동구성/관측/마이그레이션) — subprojects 인프라에 없는 것만.
    implementation(libs.exposed.spring.boot.starter)
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation(libs.sentry.spring.boot)
    implementation("org.flywaydb:flyway-database-postgresql")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // 전체 테스트가 이 모듈에 있다 — 모든 레이어/인프라 테스트 의존을 여기서 제공.
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

// 메인 test: ArchUnit·OpenSearch 실연결 통합테스트는 제외(각각 전용 태스크에서만).
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

// ArchUnit 아키텍처 경계 규칙만 실행(DB 불필요). 모듈로 승격 못 한 규칙(레이어 경계, 상대 inbound 포트만 등)을 계속 강제.
tasks.register<Test>("archTest") {
    description = "ArchUnit 아키텍처 경계 규칙만 실행"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    filter { includeTestsMatching("com.example.backend.architecture.*") }
}

// 전체 테스트는 이 모듈에 있으나 코드는 도메인 모듈들에 흩어져 있다 — 집계 리포트를 만든다.
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    val coveredModules =
        listOf("common", "user-api", "area", "media", "direction", "place", "course", "user", "mobile", "bootstrap")
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

// bootstrap 은 OpenSearch/SQS/S3 config 빈을 모두 가진다.
dependencies {
    implementation(libs.opensearch.java)
    implementation(platform(libs.spring.cloud.aws.bom))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    implementation(platform(libs.aws.bom))
    implementation(libs.aws.s3)
    // OpenSearch IAM/SigV4 전송(AwsSdk2Transport)에 필요한 동기 SdkHttpClient. 버전은 AWS SDK BOM 관리.
    implementation("software.amazon.awssdk:apache-client")
}
