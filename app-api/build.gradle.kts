// 어플리케이션 계층: 실행 가능한 API 서버. 하위 모듈(core-web, client-*, domain-rds, common)을 조립해 서비스를 완성한다.
// 포함: main, 앱 버전 정책, JWT 발급/검증 빈, 인바운드 어댑터(컨트롤러, SQS 소비), BFF(mobile),
//       도메인 포트를 구현하는 얇은 아웃바운드 어댑터(client-* 를 써서 외부 응답을 도메인 타입으로 옮긴다),
//       DB(Exposed) 예외 변환, 색인 초기화/재색인, 전체 테스트.
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
    implementation(project(":core-web"))
    implementation(project(":client-opensearch"))
    implementation(project(":client-s3"))
    implementation(project(":client-sqs"))
    implementation(project(":client-kakao"))
    implementation(project(":client-tmap"))
    implementation(project(":client-google"))
    implementation(project(":client-naver"))
    implementation(project(":client-fcm"))

    // 웹, 보안, 검증(필터 체인, 예외 핸들러, OpenAPI, Sentry 설정은 core-web 이 가진다)
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.springdoc.openapi.webmvc.ui)

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
    // Spring Modulith 이벤트 저장소(JDBC): 이벤트를 업무 변경과 같은 트랜잭션으로 event_publication 에 남기는 아웃박스.
    // starter-jdbc 대신 이벤트 모듈만 쓴다. starter-core 가 끌어오는 spring-modulith-core(ArchUnit, jMolecules) 를
    // 런타임(bootJar)에 싣지 않기 위해서다. 모듈 검증용 core 는 테스트(starter-test)에만 둔다.
    implementation(platform(libs.spring.modulith.bom))
    implementation(libs.spring.modulith.events.api)
    // events-core 가 끌어오는 jcl-over-slf4j 는 이미 있는 commons-logging 과 같은 클래스를 중복 제공해 뺀다
    implementation(libs.spring.modulith.events.jdbc) { exclude(group = "org.slf4j", module = "jcl-over-slf4j") }
    implementation(libs.spring.modulith.events.jackson) { exclude(group = "org.slf4j", module = "jcl-over-slf4j") }
    implementation("org.postgresql:postgresql")

    // S3, SQS, FCM SDK 는 각 client-* 모듈이 가진다. AWS BOM 은 아래 DocumentDB 인증(auth)용.
    implementation(platform(libs.aws.bom))

    // DocumentDB(MongoDB 호환): 연결 설정은 bootstrap/config/MongoConfig. MONGODB-AWS 인증은 SDK v2 기본 자격증명 체인(EC2 인스턴스 역할)
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("software.amazon.awssdk:auth")
    // ElastiCache Redis(IAM 인증): 연결 설정은 bootstrap/config/RedisConfig
    implementation("io.lettuce:lettuce-core")

    // 관측
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation(libs.archunit.junit5)
    // Spring Modulith: 도메인 패키지(모듈) 경계와 순환 검증. archTest 에서 돈다
    testImplementation(platform(libs.spring.modulith.bom))
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
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
    val coveredModules =
        listOf(
            "common",
            "domain-rds",
            "core-web",
            "client-opensearch",
            "client-s3",
            "client-sqs",
            "client-kakao",
            "client-tmap",
            "client-google",
            "client-naver",
            "client-fcm",
            "app-api",
        )
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
