// 내부 계층(client-kakao): 카카오 로컬 API(키워드 검색, 좌표→행정구역) 클라이언트와 응답 DTO, RestClient 빈.
// 원칙: 도메인을 모른다. 장소, 카테고리 변환은 이 클라이언트를 쓰는 쪽(app-api 어댑터)이 가진다.
plugins {
    id("backend.kotlin-conventions")
}

dependencies {
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot")
    implementation("com.fasterxml.jackson.core:jackson-annotations")
}
