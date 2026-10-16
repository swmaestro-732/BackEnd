rootProject.name = "Backend"

// 계층별 모듈 — 우아한형제들 「멀티모듈 설계 이야기」 기준. 위 계층만 아래 계층에 의존한다.
// 공통
include("common")
// 독립
include("exposed-postgis")
// 도메인
include("domain-rds", "domain-search")
// 내부
include("core-web", "client-tmap", "client-kakao", "client-oauth", "client-s3")
