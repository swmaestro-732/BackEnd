plugins {
    id("backend.kotlin-conventions")
}

description = "user 컨텍스트의 공개 계약(Published Language) — 도메인 모델 + 인바운드 포트·DTO. 타 컨텍스트는 이 모듈만 의존한다."

dependencies {
    api(project(":common"))

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
