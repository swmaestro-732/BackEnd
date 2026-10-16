# domain-rds

- **계층**: 도메인
- **역할**: 중심 도메인 + RDS. 모든 바운디드 컨텍스트의 domain·application(포트·서비스), Exposed 영속 어댑터, 컨텍스트 간 in-process 어댑터, PostGIS 컬럼 타입, Flyway 마이그레이션. 컨텍스트 간 경계는 ArchUnit(app-api 의 archTest)이 강제한다.
- **의존 가능**: common
