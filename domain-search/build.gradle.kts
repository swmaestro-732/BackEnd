plugins {
    id("backend.library-conventions")
}

// 검색 저장소(OpenSearch) — domain-rds 의 검색 포트를 구현한다. 모듈 하나 = 인프라 하나.
dependencies {
    implementation(project(":domain-rds"))

    // OpenSearch(AWS) 연결 — 기본 FGAC basic auth(ApacheHttpClient5), 선택 IAM/SigV4(AwsSdk2Transport).
    // 사용하지 않는 구형 RestClient 전송(opensearch-rest-client)은 제외한다.
    implementation(libs.opensearch.java) {
        exclude(group = "org.opensearch.client", module = "opensearch-rest-client")
    }
    implementation("org.apache.httpcomponents.client5:httpclient5")
    // OpenSearch IAM/SigV4 전송에 필요한 동기 SdkHttpClient.
    implementation(platform(libs.aws.sdk.bom))
    implementation("software.amazon.awssdk:apache-client")
    implementation("software.amazon.awssdk:auth")
    implementation("software.amazon.awssdk:regions")
    // opensearch-java 의 JacksonJsonpMapper 는 Jackson 2(com.fasterxml)를 쓴다 — 검색 응답을 Kotlin data class 로
    // 역직렬화하려면 Jackson 2 용 kotlin 모듈이 필요하다. 버전은 opensearch-java 가 끌어오는 jackson-databind 에 맞춘다.
    implementation(libs.jackson2.module.kotlin)
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-health")
    implementation("org.springframework:spring-tx")
    implementation(libs.kotlin.logging)
}
