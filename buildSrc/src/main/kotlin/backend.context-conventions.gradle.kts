// 어댑터(web/persistence)를 가진 컨텍스트 모듈 규칙: 공통 규칙 + 어댑터 인프라.
// 도메인 모듈(area, media, direction, place, course, user, mobile, report)과 :bootstrap 이 적용한다.
// :common(공유 커널), :user-api(순수 계약)는 적용하지 않고 각자 필요한 것만 선언한다.
// 특정 외부 SDK(OpenSearch/SQS/S3)는 여기 넣지 않고 실제 쓰는 모듈에서 직접 선언한다(범위 축소).
plugins {
    id("backend.kotlin-conventions")
}

val libs = versionCatalogs.named("libs")

fun lib(alias: String) = libs.findLibrary(alias).get()

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework:spring-tx")
    implementation(lib("exposed-core"))
    implementation(lib("exposed-jdbc"))
    implementation(lib("exposed-dao"))
    implementation(lib("exposed-kotlin-datetime"))
    implementation(lib("springdoc-openapi-webmvc-ui"))
    implementation("org.apache.httpcomponents.client5:httpclient5")
    implementation("org.postgresql:postgresql")
}
