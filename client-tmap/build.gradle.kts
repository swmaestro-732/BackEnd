plugins {
    id("backend.library-conventions")
}

// T map 보행자 경로 API 클라이언트.
dependencies {
    api(project(":common"))

    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework:spring-web")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation(libs.kotlin.logging)
}
