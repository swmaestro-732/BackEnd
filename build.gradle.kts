// 공통 빌드 규칙은 buildSrc 의 컨벤션 플러그인이 담당한다. 각 모듈이 build.gradle.kts 에서 직접 고른다.
// - backend.kotlin-conventions: 모든 모듈 공통(JDK, Spring BOM, CVE 오버라이드, 컴파일 옵션, 테스트 env)
// - backend.context-conventions: 위 + 어댑터 인프라(web/security/Exposed 등). 도메인 모듈과 :bootstrap
// 플러그인 버전은 gradle/libs.versions.toml 에서 buildSrc 가 가져오므로 여기 plugins {} 를 두지 않는다.
