import org.springframework.boot.gradle.plugin.SpringBootPlugin

// 모든 모듈 공통 — Kotlin/JVM 21, Spring Boot BOM 버전 관리, JUnit5.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("io.spring.dependency-management")
    jacoco
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// 보안 패치(SCRUM-520): Spring Boot 4.1.0 BOM 이 번들하는 취약 버전을 패치 버전으로 오버라이드한다.
// trivy HIGH/CRITICAL(Tomcat/Netty/httpcore5/postgresql) 대응. 기능 변경 없음. CVE DB 갱신으로 게이트가 막혀 올린다.
extra["tomcat.version"] = "11.0.25" // CVE-2026-65182/65905/68525 (CRITICAL)
extra["netty.version"] = "4.2.17.Final" // CVE-2026-59901/55831/55833/56745/56819 (HIGH) + GHSA-fccg-mwvh-qqg4
extra["httpcore5.version"] = "5.4.3" // CVE-2026-54399/54428 (HIGH)
extra["postgresql.version"] = "42.7.12" // CVE-2026-54291 (HIGH)
// CVE-2026-47884 (CRITICAL) — spring-webmvc XsltView RCE. Boot 4.1.0 번들 7.0.8 오버라이드.
extra["spring-framework.version"] = "7.0.9"

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}

kotlin {
    compilerOptions {
        // Spring Boot 플러그인이 app 모듈에만 켜 주는 설정 — 라이브러리 모듈도 파라미터 이름을 보존한다(AOP·MVC 바인딩).
        javaParameters = true
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

dependencies {
    "testImplementation"("org.jetbrains.kotlin:kotlin-test-junit5")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
