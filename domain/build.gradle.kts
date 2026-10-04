// :domain — 순수 도메인 코어. Spring/Exposed 무의존(컴파일 타임 강제). :common 만 의존한다.
// 여기에 Spring/Exposed/web 의존을 추가하면 헥사고날 순수성이 깨진다 — 추가 금지.
dependencies {
    implementation(project(":common"))
    // 순수 날짜 타입(kotlinx.datetime.LocalDate). Exposed 가 아니라 kotlinx 라이브러리 자체라 도메인 순수성 유지.
    // 버전은 adapter 의 exposed-kotlin-datetime 이 끌어오는 것과 일치시킨다.
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")
}
