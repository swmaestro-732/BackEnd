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
    // org.springframework.boot 플러그인(bootJar/bootRun)은 :bootstrap 에서만 적용한다.

    // 보안 패치(SCRUM-520): Spring Boot 4.1.0 BOM 번들 취약 버전을 패치 버전으로 오버라이드.
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

    // opensearch-java 는 ApacheHttpClient5 전송만 쓴다 — 구형 rest-client(httpclient4 스택)를 전역 제외(중복 무게 제거).
    configurations.all {
        exclude(group = "org.opensearch.client", module = "opensearch-rest-client")
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    // 공유 인프라 의존을 한곳에 둔다(모듈별 중복 제거). 버전은 카탈로그(libs) 또는 BOM 이 관리.
    // 도메인 모듈화에서 레이어 순수성은 각 모듈 내 ArchUnit 로 강제하므로, 전 모듈이 인프라를 갖는 것은 무방.
    val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

    fun lib(alias: String) = libs.findLibrary(alias).get()
    dependencies {
        add("implementation", "org.jetbrains.kotlin:kotlin-reflect")

        add("implementation", "org.springframework.boot:spring-boot-starter-webmvc")
        add("implementation", "org.springframework.boot:spring-boot-starter-security")
        add("implementation", "org.springframework.boot:spring-boot-starter-oauth2-resource-server")
        add("implementation", "org.springframework.boot:spring-boot-starter-validation")
        add("implementation", "org.springframework.boot:spring-boot-starter-aspectj")
        add("implementation", "org.springframework:spring-tx")

        add("implementation", lib("exposed-core"))
        add("implementation", lib("exposed-jdbc"))
        add("implementation", lib("exposed-dao"))
        add("implementation", lib("exposed-kotlin-datetime"))
        add("implementation", lib("kotlinx-datetime"))

        add("implementation", "tools.jackson.module:jackson-module-kotlin")
        add("implementation", lib("jackson-module-kotlin-v2"))
        add("implementation", lib("kotlin-logging"))
        add("implementation", lib("springdoc-openapi-webmvc-ui"))

        add("implementation", platform(lib("aws-bom")))
        add("implementation", lib("aws-s3"))
        add("implementation", platform(lib("spring-cloud-aws-bom")))
        add("implementation", "io.awspring.cloud:spring-cloud-aws-starter-sqs")

        add("implementation", lib("opensearch-java"))
        add("implementation", "org.apache.httpcomponents.client5:httpclient5")
        add("implementation", "org.postgresql:postgresql")

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
