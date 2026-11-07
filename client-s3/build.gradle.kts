// 내부 계층(client-s3): S3 연결 설정(S3Client, S3Presigner 빈)과 객체 작업 클라이언트(프리사인 PUT, 삭제).
// 원칙: 도메인을 모른다. 버킷, 키, 만료 같은 값은 이 클라이언트를 쓰는 쪽(app-api 어댑터)이 정한다.
plugins {
    id("backend.kotlin-conventions")
}

dependencies {
    // S3Client, S3Presigner 타입이 이 모듈의 공개 API(S3StorageClient 생성자)라 소비자에게 노출한다.
    api(platform(libs.aws.bom))
    api(libs.aws.s3)
    implementation("org.springframework:spring-context")
}
