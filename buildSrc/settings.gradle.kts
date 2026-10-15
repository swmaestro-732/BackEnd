// buildSrc 도 루트와 같은 버전 카탈로그를 쓴다(플러그인 버전의 단일 출처).
dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
