plugins {
    id("backend.domain-conventions")
}

description = "place 컨텍스트 — 장소 조회·검색(OpenSearch·카카오 로컬)·리뷰·지도 그리드"

dependencies {
    implementation(project(":area"))

    // OpenSearch(AWS) — opensearch-rest-client 제외는 backend.domain-conventions 가 담당.
    implementation("org.opensearch.client:opensearch-java:2.25.0")
    implementation("org.apache.httpcomponents.client5:httpclient5")
    // opensearch-java 의 JacksonJsonpMapper 는 Jackson 2 — 검색 응답 data class 역직렬화용 Kotlin 모듈(버전은 opensearch-java 와 정합).
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.4")
}
