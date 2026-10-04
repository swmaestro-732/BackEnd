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
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    // 보안 패치(SCRUM-520): Spring Boot 4.1.0 BOM 번들 취약 버전을 패치 버전으로 오버라이드.
    extra["tomcat.version"] = "11.0.25"
    extra["netty.version"] = "4.2.17.Final"
    extra["httpcore5.version"] = "5.4.3"
    extra["postgresql.version"] = "42.7.12"

    configure<JavaPluginExtension> {
        toolchain { languageVersion = JavaLanguageVersion.of(21) }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    // 실행 가능한 산출물(bootJar)은 :bootstrap 만. 라이브러리 모듈은 bootJar 끄고 일반 jar 켠다.
    if (name != "bootstrap") {
        tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") { enabled = false }
        tasks.named<Jar>("jar") { enabled = true }
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
