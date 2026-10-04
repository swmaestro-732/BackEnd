# 멀티모듈 전환 계획 (ArchUnit → 실제 Gradle 멀티모듈)

## 배경
현재 단일 모듈 + ArchUnit 테스트로 헥사고날 레이어/크로스도메인 경계를 "런타임 테스트"로만 강제.
목표: 경계를 **컴파일 타임**으로 강제하도록 실제 Gradle 멀티모듈로 전환.

## 모듈 DAG (5모듈, 레이어 기반)
```
:common  (framework-free: response/exception/geo/web/mock/domain.CourseVisibility)
   ^
:domain  (전 컨텍스트 domain/, Spring/Exposed 무의존 = 순수 코어)   -> :common
   ^
:application (전 컨텍스트 application/ + common.support.TransactionSupport) -> :domain, :common
   ^
:adapter (전 컨텍스트 adapter/ + mobile/ + common.persistence.postgis) -> :application, :domain, :common
   ^
:bootstrap (bootstrap/ + BackendApplication + resources + 전체 테스트) -> 전부, bootJar
```

## 컴파일 강제되는 경계(= ArchUnit에서 승격)
- 도메인은 application/adapter/Spring/Exposed에 의존 불가 (:domain 의존이 :common뿐)
- 애플리케이션은 어댑터에 의존 불가 (:application이 :adapter 미의존)
- common은 도메인 미참조 (구조적 리프)

## 여전히 ArchUnit로 남기는 규칙(모듈로는 못 잡음)
- inbound ↛ outbound (둘 다 :adapter) — 추후 :adapter-inbound/:adapter-outbound 분리로 승격 가능
- 어댑터는 service 구현이 아니라 port에 의존 (서브패키지 제약)
- 크로스도메인 격리(도메인 코어는 타 도메인 미참조) — 컨텍스트 모듈화 시 승격 가능
→ ArchUnit 테스트는 :bootstrap/src/test 에 유지해 계속 돌린다.

## 배치 결정 근거
- TransactionSupport: Spring-tx만 사용(Exposed 아님), application(MediaService)이 사용 → :application
- postgis(Geography/PostGisFunctions): Exposed 사용, outbound 어댑터만 → :adapter
- domain의 common import: BusinessException/ErrorCode/Coordinate/CourseVisibility (전부 framework-free) → :common 리프로 충분

## 테스트 전략
- v1: 전체 테스트를 :bootstrap/src/test 로 이동(의존이 전부라 그대로 컴파일/실행). 재배치 리스크 최소화.
- test/archTest/opensearchIt 태스크 필터 + jacoco 는 :bootstrap 에 둔다.
- 추후 모듈별 테스트 소스셋 분리는 후속 과제.

## 실행 순서
1. settings.gradle.kts (include 5 모듈)
2. 루트 build.gradle.kts (subprojects 공통 설정: kotlin+spring plugin, dependency-management BOM, 보안 version override, toolchain, 공통 테스트 env)
3. 모듈별 build.gradle.kts (의존 서브셋)
4. git mv: main 소스를 레이어별 모듈 src 로 이동(패키지명 불변 → import 수정 불필요)
5. resources + 전체 test → :bootstrap
6. 컴파일(:bootstrap:assemble) → 오류 수정 → ./gradlew test archTest 통과
7. CI 워크플로 태스크 경로 점검

## 리스크/완화
- Gradle BOM/plugin 배선 반복 → 컴파일 체크포인트로 조기 포착
- 패키지명 불변이라 import 변경 없음(핵심 리스크 감소)
- split-package(여러 모듈이 같은 패키지) 존재 — JPMS 미사용이라 런타임 무해, 단 같은 FQN 중복은 없음(파일 단위 분할)
