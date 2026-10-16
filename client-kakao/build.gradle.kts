plugins {
    id("backend.library-conventions")
}

// 카카오 로컬(키워드 검색·좌표→법정동) API 클라이언트.
dependencies {
    api(project(":common"))

    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework:spring-web")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation(libs.kotlin.logging)
}
