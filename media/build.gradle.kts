dependencies {
    implementation(project(":common"))
}

// S3(미디어 업로드)
dependencies {
    implementation(platform(libs.aws.bom))
    implementation(libs.aws.s3)
}
