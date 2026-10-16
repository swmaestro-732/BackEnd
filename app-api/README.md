# app-api

- **계층**: 어플리케이션
- **역할**: 실행 모듈(bootJar). 컨트롤러·mobile BFF, 도메인 포트를 client 로 잇는 접착 어댑터, 소셜 검증기, JwtTokenProvider, GlobalExceptionHandler, SQS 발행·수신, AppVersion 인터셉터, application.yml, @SpringBootTest 통합 테스트.
- **의존 가능**: 모든 하위 모듈
