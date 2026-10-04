// :application — 유스케이스(포트 + 서비스) + common.support(TransactionSupport, Spring-tx).
// :domain, :common 에 의존. 어댑터/Exposed/web 에는 의존하지 않는다(컴파일 타임 강제).
dependencies {
    implementation(project(":domain"))
    implementation(project(":common"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    // 날짜 변환 확장(toJavaLocalDate/toKotlinLocalDate). kotlinx 라이브러리 자체(프레임워크 아님).
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")
    // @ConfigurationProperties 등 스프링 부트 프로퍼티 바인딩(MediaProperties 가 이 모듈로 이동).
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
}
