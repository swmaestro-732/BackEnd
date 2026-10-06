plugins {
    id("backend.domain-conventions")
}

description = "user 컨텍스트 — 계정·소셜 로그인·JWT·팔로우·저장함(장소/코스/폴더)·코스 개수 집계"

dependencies {
    api(project(":user-api"))
    implementation(project(":course")) // 저장함·정리·개수 집계가 course 인바운드 포트 사용(ACL 어댑터)
    implementation(project(":place"))
    implementation(project(":media"))
    implementation(project(":area"))

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server") // JWT 발급·검증(nimbus)
    // SQS — 코스 개수 폴백 큐. spring-cloud-aws 4.0.x = Spring Boot 4 + Jackson 3.
    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies:4.0.2"))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
}
