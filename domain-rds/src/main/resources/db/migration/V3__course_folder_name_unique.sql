-- 코스 저장 폴더 실구현(SCRUM-426).
-- V2 는 장소 저장 유니크 인덱스(SCRUM-427)가 선점했다 — 두 브랜치가 같은 V2 를 쓰면 파일명이 달라
-- git 충돌 없이 머지된 뒤 Flyway 가 "Found more than one migration with version 2" 로 부팅에 실패한다.
-- ⚠️ out-of-order 는 꺼져 있으므로 **SCRUM-427(V2)이 먼저 머지된 뒤** 이 V3 이 머지돼야 한다.
-- 같은 사용자 안에서 폴더 이름은 유일하다 — 폴더 칩이 이름으로만 구분되므로 중복 생성을 막는다.
-- 애플리케이션이 먼저 중복을 검사하고, 이 인덱스는 동시 생성 경합을 막는 최종 방어선이다
-- (위반 시 GlobalExceptionHandler 가 409 FOLDER_NAME_ALREADY_TAKEN 으로 변환한다).
CREATE UNIQUE INDEX saved_course_folders_user_id_name_key
    ON public.saved_course_folders (user_id, name);
