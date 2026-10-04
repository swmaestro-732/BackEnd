import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    kotlin("jvm") version "2.3.21" apply false
    kotlin("plugin.spring") version "2.3.21" apply false
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.example"
    version = "0.0.1-SNAPSHOT"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "io.spring.dependency-management")
    // org.springframework.boot 플러그인(bootJar/bootRun 생성)은 :bootstrap 에서만 적용한다.
    // 라이브러리 모듈은 BOM import 로 버전 관리만 받고, 실행 가능한 산출물은 만들지 않는다.

    // 보안 패치(SCRUM-520): Spring Boot 4.1.0 BOM 번들 취약 버전을 패치 버전으로 오버라이드.
    // io.spring.dependency-management 는 BOM 프로퍼티를 프로젝트 ext 로 덮어쓴다.
    extra["tomcat.version"] = "11.0.25"
    extra["netty.version"] = "4.2.17.Final"
    extra["httpcore5.version"] = "5.4.3"
    extra["postgresql.version"] = "42.7.12"

    configure<DependencyManagementExtension> {
        imports { mavenBom(SpringBootPlugin.BOM_COORDINATES) }
    }

    configure<JavaPluginExtension> {
        toolchain { languageVersion = JavaLanguageVersion.of(21) }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    dependencies {
        add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
        add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
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
}
