// 내부 계층(client-fcm): Firebase 연결 설정(FirebaseApp, FirebaseMessaging 빈)과 멀티캐스트 발송 클라이언트(FcmSender).
// 원칙: 도메인을 모른다. 토큰별 결과 해석(무효 토큰, 재시도)은 이 클라이언트를 쓰는 쪽(app-api 어댑터)이 가진다.
plugins {
    id("backend.kotlin-conventions")
}

dependencies {
    // 발송 결과의 MessagingErrorCode 와 전파되는 FirebaseMessagingException 이 공개 API 라 소비자에게 노출한다.
    api(libs.firebase.admin)
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
