// 내부 계층(client-tmap): T map 보행자 경로 API 클라이언트와 요청/응답 DTO, RestClient 빈.
// 원칙: 도메인을 모른다. 경로 판단(도보 가능, 불가, 모름)과 도메인 변환은 이 클라이언트를 쓰는 쪽(app-api 어댑터)이 가진다.
plugins {
    id("backend.kotlin-conventions")
}

dependencies {
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot")
    // 응답 좌표(coordinates)가 Point, LineString 마다 모양이 달라 JsonNode 로 받는다. 소비자도 이 타입을 읽는다.
    api("tools.jackson.core:jackson-databind")
}
