# BackEnd — AI 에이전트 지침

> 이 파일이 원본입니다. `CLAUDE.md`는 이 파일을 임포트합니다 (Claude Code·Codex 공통 진입점).

## 단일 진실원: `.ai` 위키

- 이 레포의 규칙·명세·스킬의 **단일 진실원은 `.ai` 위키**다 (심링크 `./.ai`, 없으면 `../.ai` → `~/.ai` 순으로 찾고, 그래도 없으면 GitHub `swmaestro-732/.ai`).
- 팀 공통 지침(프로젝트 개요·인코딩·금지사항·협업 방식)은 **[`.ai/AGENTS.md`](.ai/AGENTS.md)**를 따른다. 이 파일은 그 위에 BackEnd 작업 진입점만 얹는다.

## 작업 시작 전 읽을 문서

- `.ai/conventions.md` — 공통 컨벤션(브랜치·커밋·PR)
- `.ai/backend/code-convention.md` — 백엔드 코드 컨벤션(응답 엔벨로프·에러 코드·모킹 API 등)
- `.ai/backend/architecture.md` — 헥사고날 구조·보안
- `.ai/api-spec.md` · `.ai/api-design.md` — API 카탈로그·설계 원칙

## 반복 작업은 스킬 절차로

`.ai/skills/<name>/SKILL.md`를 읽고 그 절차를 따른다:

- 모킹 API 생성 → `.ai/skills/mock-api`
- 브랜치·커밋·PR → `.ai/skills/pr-flow`
- 커밋 전 위키 갱신 감지 → `.ai/skills/wiki-check`
- 로컬 기동·검증(빌드/테스트 체인) → `.ai/skills/backend-run`
- 역할 스킬(PM·디자인 QA·QA·아키텍트·시큐리티) → `.ai/skills/`

## 원칙

- 스킬·규칙 수정은 **`.ai` 원본에서** 한다 — 레포에 사본을 두지 않는다(위키가 단일 진실원).
- Claude Code는 `.claude/skills/`에 설치 시 슬래시 명령으로도 호출 가능, Codex는 `.ai/skills/<name>/SKILL.md`를 직접 읽어 따른다.
