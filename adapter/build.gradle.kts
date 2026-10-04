// :adapter — 인바운드(web) + 아웃바운드(persistence/search/social/media) 어댑터 + mobile BFF + common.persistence(PostGIS).
// :application, :domain, :common 에 의존. 인프라 라이브러리(web/security/Exposed/OpenSearch/AWS)를 여기서 쓴다.
dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    implementation(project(":common"))

    // Exposed (영속성 어댑터)
    implementation("org.jetbrains.exposed:exposed-jdbc:1.3.0")
    implementation("org.jetbrains.exposed:exposed-core:1.3.0")
    implementation("org.jetbrains.exposed:exposed-dao:1.3.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:1.3.0")

    // 트랜잭션(@Transactional, @TransactionalEventListener, TransactionPhase) — 검색 동기화 리스너/화면 서비스에서 사용
    implementation("org.springframework:spring-tx")
    implementation("org.springframework:spring-context")

    // web / security / 검증
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")

    // 직렬화 / 로깅
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.4")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // OpenAPI (컨트롤러 애노테이션)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    // AWS SDK (S3)
    implementation(platform("software.amazon.awssdk:bom:2.49.0"))
    implementation("software.amazon.awssdk:s3")

    // SQS (코스 개수 폴백 큐)
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:4.0.2"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")

    // OpenSearch 연결
    implementation("org.opensearch.client:opensearch-java:2.25.0") {
        exclude(group = "org.opensearch.client", module = "opensearch-rest-client")
    }
    implementation("org.apache.httpcomponents.client5:httpclient5")

    implementation("org.postgresql:postgresql")
}
