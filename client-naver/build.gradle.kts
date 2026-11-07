// 내부 계층(client-naver): 네이버 회원 프로필 API 클라이언트와 응답 DTO, RestClient 빈.
// 원칙: 도메인을 모른다. 소셜 로그인 검증(결과 코드 해석, 오류 분류)은 이 클라이언트를 쓰는 쪽(app-api 어댑터)이 가진다.
plugins {
    id("backend.kotlin-conventions")
}

dependencies {
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
}
