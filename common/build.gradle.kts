// :common - 공유 커널(response/exception/geo/web/mock) + 공통 웹/보안 애노테이션 + 설정 프로퍼티 + PostGIS/트랜잭션 헬퍼.
// 어댑터 인프라 일괄 제공(subprojects)에서 제외되며, 여기 내용이 실제 쓰는 최소 인프라만 선언한다.
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework:spring-tx")
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    // GeographyPointColumnType 가 PGobject 로 PostGIS geography 를 바인딩한다.
    implementation("org.postgresql:postgresql")
}
