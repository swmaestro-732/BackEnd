// 내부 계층(client-google): Google Places API(New) 텍스트 검색 클라이언트와 응답 DTO, RestClient 빈.
// 원칙: 도메인을 모른다. 동명이소 판단과 영업시간 변환은 이 클라이언트를 쓰는 쪽(app-api 어댑터)이 가진다.
plugins {
    id("backend.kotlin-conventions")
}

dependencies {
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot")
}
