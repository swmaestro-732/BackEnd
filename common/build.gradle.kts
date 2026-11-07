// 공통 계층: 모든 모듈이 쓰는 타입(응답 엔벨로프, 에러 코드, 예외, 좌표, 공통 enum).
// 원칙: 외부 의존을 두지 않는다. 예외는 ApiResponse 의 @JsonInclude 애노테이션 하나.
plugins {
    id("backend.kotlin-conventions")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations")
}
