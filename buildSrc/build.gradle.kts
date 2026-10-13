plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

// 컨벤션 플러그인(backend.kotlin-conventions)이 쓰는 Gradle 플러그인 — 버전은 gradle/libs.versions.toml 이 관리한다.
dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen) // kotlin("plugin.spring")
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.spring.dependency.management.plugin)
}
