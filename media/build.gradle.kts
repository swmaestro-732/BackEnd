plugins {
    id("backend.domain-conventions")
}

description = "media 컨텍스트 — S3 프리사인 업로드"

dependencies {
    implementation(platform(libs.aws.bom))
    implementation("software.amazon.awssdk:s3")
}
