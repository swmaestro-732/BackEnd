// 실행 jar 가 없는 하위 모듈(공통·독립·도메인·내부) — 노출할 의존만 api, 나머지는 implementation.
plugins {
    id("backend.kotlin-conventions")
    `java-library`
}
