// :adapter — 인바운드(web) + 아웃바운드(persistence/search/social/media) 어댑터 + mobile BFF + common.persistence(PostGIS).
// :application, :domain, :common 에 의존. 인프라 라이브러리(web/security/Exposed/OpenSearch/AWS)를 여기서 쓴다.
dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))
    implementation(project(":common"))

    // Exposed (영속성 어댑터)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)

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
    implementation(libs.jackson.module.kotlin.v2)
    implementation(libs.kotlin.logging)

    // OpenAPI (컨트롤러 애노테이션)
    implementation(libs.springdoc.openapi.webmvc.ui)

    // AWS SDK (S3)
    implementation(platform(libs.aws.bom))
    implementation(libs.aws.s3)

    // SQS (코스 개수 폴백 큐)
    implementation(platform(libs.spring.cloud.aws.bom))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")

    // OpenSearch 연결
    implementation(libs.opensearch.java) {
        exclude(group = "org.opensearch.client", module = "opensearch-rest-client")
    }
    implementation("org.apache.httpcomponents.client5:httpclient5")

    implementation("org.postgresql:postgresql")
}
