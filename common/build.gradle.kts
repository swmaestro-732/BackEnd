// :common — framework-free 공유 커널 (response 엔벨로프/에러코드, exception, geo, web, mock, 공통 도메인 enum).
// Spring 컨텍스트/Exposed 무의존. 이 모듈에 그런 의존을 추가하지 말 것(도메인이 이 모듈에만 기대므로 순수성 유지).
dependencies {
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
}
