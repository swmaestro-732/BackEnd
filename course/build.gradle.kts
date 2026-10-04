plugins {
    id("backend.domain-conventions")
}

description = "course 컨텍스트 — 코스·플랜·리뷰·댓글·좋아요·검색 색인"

dependencies {
    implementation(project(":user-api")) // 뷰어 상호작용(좋아요/저장 여부) 조회 ACL 어댑터
    implementation(project(":place"))
    implementation(project(":area"))

    // OpenSearch(AWS) — ApacheHttpClient5 전송만 사용. 구형 RestClient 가 끌고 오는 httpclient 4.x 스택 제외.
    implementation("org.opensearch.client:opensearch-java:2.25.0") {
        exclude(group = "org.opensearch.client", module = "opensearch-rest-client")
    }
    implementation("org.apache.httpcomponents.client5:httpclient5")
    // opensearch-java 의 JacksonJsonpMapper 는 Jackson 2 — 검색 응답 data class 역직렬화용 Kotlin 모듈(버전은 opensearch-java 와 정합).
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.4")
}
