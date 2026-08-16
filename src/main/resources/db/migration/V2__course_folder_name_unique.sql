-- 코스 저장 폴더 실구현(SCRUM-426).
-- 같은 사용자 안에서 폴더 이름은 유일하다 — 폴더 칩이 이름으로만 구분되므로 중복 생성을 막는다.
-- 애플리케이션이 먼저 중복을 검사하고, 이 인덱스는 동시 생성 경합을 막는 최종 방어선이다
-- (위반 시 GlobalExceptionHandler 가 409 FOLDER_NAME_ALREADY_TAKEN 으로 변환한다).
CREATE UNIQUE INDEX saved_course_folders_user_id_name_key
    ON public.saved_course_folders (user_id, name);
