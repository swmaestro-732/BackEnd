rootProject.name = "Backend"

// 계층별 모듈 — 위 계층만 아래 계층에 의존한다.
// 어플리케이션
include("app-api")
// 공통
include("common")
// 도메인
include("domain-rds", "domain-search")
// 내부
include("core-web", "client-tmap", "client-kakao", "client-oauth", "client-s3")
