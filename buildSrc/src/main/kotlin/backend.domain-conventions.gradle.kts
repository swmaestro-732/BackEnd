// 바운디드 컨텍스트(도메인) 모듈 공통 규칙 — 헥사고날 한 슬라이스(domain·application·adapter)가 쓰는 기본 의존성.
// 모듈별 특수 의존성(외부 SDK 등)만 각 모듈 build.gradle.kts 에 추가한다.
plugins {
    id("backend.kotlin-conventions")
}

// opensearch-java 는 ApacheHttpClient5 전송만 쓴다 — 구형 RestClient 전송이 끌고 오는 httpclient 4.x 스택을 제외한다.
configurations.all {
    exclude(group = "org.opensearch.client", module = "opensearch-rest-client")
}

dependencies {
    api(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.jetbrains.exposed:exposed-core:1.3.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:1.3.0")
    implementation("org.jetbrains.exposed:exposed-dao:1.3.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:1.3.0")
    implementation("org.jetbrains.exposed:exposed-spring-boot-starter:1.3.0")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("tools.jackson.module:jackson-module-kotlin") // 어댑터 내부 data class(JSON 응답) 역직렬화
    implementation("com.fasterxml.jackson.core:jackson-annotations") // @JsonProperty 등(Jackson 2/3 공용)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:3.0.3") // @Tag 등 OpenAPI 어노테이션

    // 단위 테스트(컨텍스트 미부팅). @SpringBootTest 통합 테스트는 app 모듈에 둔다.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
