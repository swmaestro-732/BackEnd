rootProject.name = "Backend"

// 도메인(바운디드 컨텍스트) 단위 모듈. 레이어(domain/application/adapter)는 각 모듈 내부 패키지로 유지한다.
// 의존 방향(DAG): common <- area,media <- place <- course <- user <- mobile <- bootstrap
include(
    "common",
    "user-api",
    "area",
    "media",
    "direction",
    "place",
    "course",
    "user",
    "mobile",
    "bootstrap",
)
