import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.plugin.SpringBootPlugin

// 모든 Kotlin 모듈 공통 규칙(:common, :user-api 포함). 어댑터 인프라는 backend.context-conventions 가 더한다.
// org.springframework.boot 플러그인(bootJar/bootRun)은 :bootstrap 에서만 적용한다.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("io.spring.dependency-management")
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

repositories { mavenCentral() }

// 보안 패치(SCRUM-520): Spring Boot 4.1.0 BOM 번들 취약 버전을 패치 버전으로 오버라이드.
extra["tomcat.version"] = "11.0.25"
extra["netty.version"] = "4.2.17.Final"
extra["httpcore5.version"] = "5.4.3"
extra["postgresql.version"] = "42.7.12"
// CVE-2026-47884 (CRITICAL): spring-webmvc XsltView RCE. Boot 4.1.0 번들 7.0.8 오버라이드.
extra["spring-framework.version"] = "7.0.9"

dependencyManagement {
    imports { mavenBom(SpringBootPlugin.BOM_COORDINATES) }
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

// opensearch-java 는 ApacheHttpClient5 전송만 쓴다. 구형 rest-client(httpclient4 스택)를 전역 제외(중복 무게 제거).
configurations.all {
    exclude(group = "org.opensearch.client", module = "opensearch-rest-client")
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

// 컨벤션 플러그인에서는 libs.* 타입 안전 접근자가 생성되지 않아 카탈로그를 이름으로 조회한다.
val libs = versionCatalogs.named("libs")

fun lib(alias: String) = libs.findLibrary(alias).get()

// 모든 모듈 공통: 코틀린 리플렉션/직렬화/로깅/날짜 + 테스트 러너.
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation(lib("jackson-module-kotlin-v2"))
    implementation(lib("kotlin-logging"))
    implementation(lib("kotlinx-datetime"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // 컨텍스트 부팅에 필요한 더미 env (운영용 아님).
    environment("JWT_SECRET", "test-only-jwt-secret-not-for-production-0123456789")
    environment("S3_MEDIA_BUCKET", "test-media-bucket")
    environment("S3_MEDIA_CDN_URL", "https://cdn.test.example.com")
    environment("AWS_ACCESS_KEY_ID", "test-access-key")
    environment("AWS_SECRET_ACCESS_KEY", "test-secret-key")
}
