dependencies {
    implementation(project(":common"))
    implementation(project(":user-api"))
    implementation(project(":area"))
    implementation(project(":place"))
    implementation(project(":media"))
    implementation(project(":course"))
}

// SQS(코스 개수 폴백 큐)
dependencies {
    implementation(platform(libs.spring.cloud.aws.bom))
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
}
