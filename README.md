# 칠삼이 BackEnd

Spring Boot 기반 API 서버.

## 1. 스택

- Kotlin 2.3.21 / JDK 21
- Spring Boot 4.1.0 (WebMVC, Security, Actuator, Validation)
- Exposed 1.3.0 (ORM) + Flyway (마이그레이션) + PostgreSQL / HikariCP
- springdoc-openapi (Swagger UI, JWT Bearer)

## 2. 로컬 개발

```bash
# 1) 로컬 DB 기동 (PostgreSQL)
docker compose up -d

# 2) pre-commit 훅 설치 (최초 1회)
brew install pre-commit          # 또는 pip install pre-commit
pre-commit install               # commit-msg·pre-commit 훅 자동 설치

# 3) 실행 / 빌드
./gradlew bootRun
./gradlew build jacocoTestReport
```

- 애플리케이션: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- 헬스체크: `http://localhost:8080/actuator/health`

## 3. 컨벤션

### 3-1. 브랜치 전략 (gitflow)

```
feat/* ─▶ develop ─▶ main
```

- `main`: 항상 배포 가능한 릴리스 브랜치. 보호됨, 직접 푸시 금지.
- `develop`: 통합 브랜치. 기능은 여기로 먼저 병합.
- 작업 브랜치: `<type>/<SCRUM-키>-<요약>` — 예) `feat/SCRUM-163-cd-refresh`
- `main` 직접 머지 금지 — 반드시 `develop`을 거친다.

### 3-2. 커밋 컨벤션 (Conventional Commits)

`<type>: 내용` — `type ∈ feat · docs · fix · chore · hotfix · release`

`pre-commit`의 commit-msg 훅(`--strict`)이 형식을 강제한다.

### 3-3. PR 규칙

- 제목: `SCRUM-<번호> <type>: 내용` — 앞에 Jira 키를 붙여 자동 연결.
- 템플릿(변경 요약 / 변경 유형 / 체크리스트)을 채운다.
- **CodeRabbit 리뷰를 확인·반영한 뒤 머지한다.**
- CI 필수 체크(ktlint · build & test · docker & trivy · code security) 통과 필수.

### 3-4. 릴리스 (SemVer)

- `develop → main` PR, 제목 `release: vX.Y.Z`.
- 머지 후 `main`에 `vX.Y.Z` 태그를 부여한다 (Semantic Versioning).
- `main` 병합 시 CD가 자동 배포한다 (아래 4-2).

### 3-5. 코드 컨벤션

- ktlint 규칙 준수 (`.editorconfig`, `no-wildcard-imports`, max line 120).
- 커밋 전 pre-commit이 ktlint 포맷을 검사한다.

## 4. CI/CD

### 4-1. CI (`ci.yml`) — `main`·`develop` 대상 PR

| 체크 | 내용 |
|---|---|
| ktlint | 포맷 검사 (ktlint 1.8.0) |
| build & test | 빌드 + 테스트 + JaCoCo 커버리지 |
| docker & trivy | 이미지 빌드 + 이미지 취약점 스캔 |
| code security | Trivy fs (secret·misconfig) + Semgrep(SAST) |

### 4-2. CD (`cd.yml`) — `main` push

1. 이미지 빌드 → ECR push (`:latest`, `:<sha>`)
2. ASG **instance refresh** 트리거 → 새 인스턴스가 `:latest`를 pull, ELB 헬스체크 통과 시 무중단 롤링 교체
   - `MinHealthyPercentage=50`, `InstanceWarmup=180`
   - 진행 중 refresh가 있으면 취소·대기 후 최신 이미지로 재시작

인프라(ECR·ASG·배포 IAM)는 [`swmaestro-732/Infra`](https://github.com/swmaestro-732/Infra) 레포에서 관리한다.
