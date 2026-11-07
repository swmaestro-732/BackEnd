rootProject.name = "Backend"

// 기능(역할) 단위 멀티모듈. 계층 구조는 우아한형제들 기술블로그 "멀티모듈 설계 이야기"를 따른다.
// 계층: 어플리케이션(app-api) > 내부(core-web, client-*) > 도메인(domain-rds) > 공통(common)
// 의존은 위에서 아래로만: app-api -> core-web, client-*, domain-rds -> common. 내부 계층 모듈은 domain-rds 를 의존하지 않는다.
// 도메인(user, course, place ...) 구분은 모듈이 아니라 패키지로 하고, 경계는 ArchUnit 이 지킨다.
include(
    "common", // 공통 계층: 응답, 예외, 좌표 등 타입. 외부 의존 최소
    "domain-rds", // 도메인 계층: 모든 컨텍스트의 도메인 + 유스케이스 + PostgreSQL 영속성. 인프라는 RDB 하나
    "core-web", // 내부 계층: 웹, 보안 공통(보안 필터 체인, 인자 리졸버, 전역 예외 핸들러, OpenAPI). 도메인을 모른다
    "client-opensearch", // 내부 계층: OpenSearch 연결 설정과 클라이언트. 도메인을 모른다
    "client-s3", // 내부 계층: S3 연결 설정과 객체 작업(프리사인 PUT, 삭제) 클라이언트
    "client-sqs", // 내부 계층: SQS 범용 발행 클라이언트
    "client-kakao", // 내부 계층: 카카오 로컬 API(키워드 검색, 좌표→행정구역) 클라이언트
    "client-tmap", // 내부 계층: T map 보행자 경로 API 클라이언트
    "client-google", // 내부 계층: Google Places API(New) 텍스트 검색 클라이언트
    "client-naver", // 내부 계층: 네이버 회원 프로필 API 클라이언트
    "client-fcm", // 내부 계층: Firebase 연결 설정과 FCM 멀티캐스트 발송 클라이언트
    "app-api", // 어플리케이션 계층: 실행 가능한 API 서버. 하위 모듈을 조립한다
)
