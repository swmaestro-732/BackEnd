// 내부 계층: OpenSearch 연결 설정(basic auth 또는 IAM/SigV4 전송), 검색용/색인용 클라이언트 빈, 헬스체크.
// 원칙: 도메인과 서비스 비즈니스를 모른다. 색인 문서 매핑과 검색 쿼리는 이 클라이언트를 쓰는 쪽(app-api 어댑터)이 가진다.
// OpenSearch 가 필요 없는 실행 모듈(예: 이후 SQS worker)은 이 모듈을 의존하지 않으면 된다.
plugins {
    id("backend.kotlin-conventions")
}

dependencies {
    // OpenSearchClient 타입이 이 모듈의 공개 API 라 소비자에게 노출한다.
    api(libs.opensearch.java)
    implementation("org.apache.httpcomponents.client5:httpclient5")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-health")
    // opensearch-java 의 JacksonJsonpMapper 는 Jackson 2 를 쓴다.
    implementation(libs.jackson.module.kotlin.v2)
    // IAM/SigV4 전송: 서명(auth, regions) + 동기 SdkHttpClient(apache-client)
    implementation(platform(libs.aws.bom))
    implementation("software.amazon.awssdk:auth")
    implementation("software.amazon.awssdk:regions")
    implementation("software.amazon.awssdk:apache-client")
}
