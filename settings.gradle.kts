rootProject.name = "Backend"

// 멀티모듈(SCRUM-498). 공통 빌드 규칙은 buildSrc 의 `backend.kotlin-conventions` 플러그인이 담당한다.
include("common", "app")
