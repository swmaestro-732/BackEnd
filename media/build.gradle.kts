plugins {
    id("backend.domain-conventions")
}

description = "media 컨텍스트 — S3 프리사인 업로드"

dependencies {
    implementation(platform("software.amazon.awssdk:bom:2.49.0"))
    implementation("software.amazon.awssdk:s3")
}
