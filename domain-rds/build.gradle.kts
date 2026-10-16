plugins {
    id("backend.library-conventions")
}

// 중심 도메인 + RDS(PostgreSQL·Exposed). 모든 바운디드 컨텍스트의 domain·application 과 영속 어댑터, Flyway 마이그레이션.
dependencies {
    api(project(":common"))

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)
    // Exposed ↔ Spring 연동 (Database 자동구성 + @Transactional 지원)
    implementation(libs.exposed.spring.boot.starter)
    implementation("org.springframework.boot:spring-boot-starter-jdbc") // HikariCP 포함
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")
    implementation(libs.kotlin.logging)
}
