plugins {
    id("backend.library-conventions")
}

// S3 미디어 버킷 클라이언트 — 프리사인 PUT·CDN URL·삭제.
dependencies {
    implementation(platform(libs.aws.sdk.bom))
    implementation("software.amazon.awssdk:s3")
    implementation("org.springframework.boot:spring-boot")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation(libs.kotlin.logging)
}
