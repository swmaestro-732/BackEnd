plugins {
    id("backend.kotlin-conventions")
}

description = "공통 모듈 — 응답 엔벨로프·에러 코드·예외·좌표/PostGIS 타입과 횡단 관심사 계약(앱 버전·인증 어노테이션·공유 설정). 도메인을 참조하지 않는다."

dependencies {
    api("org.jetbrains.exposed:exposed-core:1.3.0") // Table·ColumnType 이 공개 시그니처에 노출된다
    implementation("org.jetbrains.exposed:exposed-jdbc:1.3.0")
    implementation("org.springframework:spring-tx") // TransactionSupport
    implementation("org.springframework.boot:spring-boot") // @ConfigurationProperties
    implementation("org.springframework.security:spring-security-core") // @AccessTokenRequired 의 @PreAuthorize
    implementation("org.postgresql:postgresql") // PostGIS 바이너리(PGobject)
    implementation("com.fasterxml.jackson.core:jackson-annotations") // ApiResponse 의 @JsonInclude

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
