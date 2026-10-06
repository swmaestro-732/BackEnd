import org.springframework.boot.gradle.plugin.SpringBootPlugin

// 모든 Kotlin 서브모듈 공통 규칙. Spring Boot 플러그인(bootJar)은 실행 모듈(app)에서만 추가로 적용한다.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("java-library")
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

// 보안 패치(SCRUM-520): Spring Boot 4.1.0 BOM 이 번들하는 취약 버전을 패치 버전으로 오버라이드한다.
// trivy HIGH/CRITICAL(Tomcat/Netty/httpcore5/postgresql) 대응. 기능 변경 없음. CVE DB 갱신으로 게이트가 막혀 올린다.
// 모든 모듈이 같은 버전으로 해석되도록 BOM 임포트보다 앞에 둔다.
extra["tomcat.version"] = "11.0.25" // CVE-2026-65182/65905/68525 (CRITICAL)
extra["netty.version"] = "4.2.17.Final" // CVE-2026-59901/55831/55833/56745/56819 (HIGH) + GHSA-fccg-mwvh-qqg4
extra["httpcore5.version"] = "5.4.3" // CVE-2026-54399/54428 (HIGH)
extra["postgresql.version"] = "42.7.12" // CVE-2026-54291 (HIGH)
// CVE-2026-47884 (CRITICAL) — spring-webmvc XsltView RCE. Boot 4.1.0 번들 7.0.8 오버라이드.
extra["spring-framework.version"] = "7.0.9"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // 컨텍스트 부팅에 필요한 더미 env — test·opensearchIt 공통(archTest 는 컨텍스트 미부팅이라 무시됨).
    // JWT 시크릿은 코드 기본값 없이 env 주입 — 테스트는 전용 더미(운영용 아님).
    environment("JWT_SECRET", "test-only-jwt-secret-not-for-production-0123456789")
    // S3 프리사인은 네트워크 호출 없이 로컬 서명만 계산하지만, 버킷명은 비어 있으면 SDK가 거부한다.
    environment("S3_MEDIA_BUCKET", "test-media-bucket")
    environment("S3_MEDIA_CDN_URL", "https://cdn.test.example.com")
    // DefaultCredentialsProvider 가 EC2 인스턴스 메타데이터까지 폴백하며 네트워크를 타지 않도록 더미 고정 자격증명 주입.
    environment("AWS_ACCESS_KEY_ID", "test-access-key")
    environment("AWS_SECRET_ACCESS_KEY", "test-secret-key")
}
