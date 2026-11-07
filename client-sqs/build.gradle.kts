// 내부 계층(client-sqs): SQS 범용 발행(SqsMessageSender). SqsTemplate 은 spring-cloud-aws 가 자동 배선한다.
// 원칙: 도메인을 모른다. 큐 선택, 메시지 형식, 실패 처리는 이 클라이언트를 쓰는 쪽(app-api 어댑터)이 가진다.
plugins {
    id("backend.kotlin-conventions")
}

dependencies {
    // app-api 의 SQS 소비자(@SqsListener)도 이 스타터를 쓰므로 소비자에게 노출한다.
    api(platform(libs.spring.cloud.aws.bom))
    api("io.awspring.cloud:spring-cloud-aws-starter-sqs")
}
