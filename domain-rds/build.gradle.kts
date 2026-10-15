// 도메인 계층: 모든 컨텍스트(user, course, place, area, media, direction, report)의
// domain 모델 + application(포트, 유스케이스 서비스) + PostgreSQL 영속성(Exposed) + 도메인 간 ACL 어댑터.
// 원칙: 책임지는 인프라는 RDB 하나. 웹, 보안, 검색, AWS 는 모른다(필요하면 outbound 포트로만).
// Flyway 마이그레이션(src/main/resources/db/migration)도 스키마 소유자인 이 모듈이 가진다.
plugins {
    id("backend.kotlin-conventions")
}

dependencies {
    implementation(project(":common"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.kotlinx.datetime)
    // PostGIS geography 바인딩(PGobject)
    implementation("org.postgresql:postgresql")
}
