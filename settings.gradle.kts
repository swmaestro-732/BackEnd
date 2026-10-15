rootProject.name = "Backend"

// 기능(역할) 단위 멀티모듈. 계층 구조는 우아한형제들 기술블로그 "멀티모듈 설계 이야기"를 따른다.
// 의존은 위에서 아래로만: app-api -> client-opensearch, domain-rds -> common
// 도메인(user, course, place ...) 구분은 모듈이 아니라 패키지로 하고, 경계는 ArchUnit 이 지킨다.
include(
    "common", // 공통 계층: 응답, 예외, 좌표 등 타입. 외부 의존 최소
    "domain-rds", // 도메인 계층: 모든 컨텍스트의 도메인 + 유스케이스 + PostgreSQL 영속성. 인프라는 RDB 하나
    "client-opensearch", // 내부 계층: OpenSearch 연결 설정과 클라이언트. 도메인을 모른다
    "app-api", // 어플리케이션 계층: 실행 가능한 API 서버. 하위 모듈을 조립한다
)
