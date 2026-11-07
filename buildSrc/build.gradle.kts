// 컨벤션 플러그인(src/main/kotlin/*.gradle.kts)이 적용하는 Gradle 플러그인을 클래스패스에 올린다.
// 플러그인 버전은 여기서만 정해지므로 모듈 build 파일의 plugins {} 에는 버전을 적지 않는다.
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen)
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.spring.dependency.management.plugin)
}
