-- 저장 레코드(saved_courses·saved_places) 소프트 삭제 + 코스 저장 중복 방지.
-- 하드 삭제(DELETE) 대신 deleted_at 스탬프를 찍어 이력을 남긴다(courses 와 같은 방식).

-- 1) saved_courses
-- user_id 는 V6 에서 추가됐다.
ALTER TABLE saved_courses
    ADD COLUMN deleted_at TIMESTAMPTZ;

-- 코스 저장 중복 방지: (user_id, course_id) 유니크 인덱스.
-- 서비스의 사전검사(existsSavedCourse)와 별개로, 동시 저장 경합(TOCTOU)에서도
-- 코스당 저장 레코드가 1개임을 DB 가 보장한다.
-- 위반(SQLSTATE 23505)은 GlobalExceptionHandler 가 인덱스명(saved_courses)으로 식별해
-- 4093(COURSE_ALREADY_SAVED)로 변환한다.
-- 유니크 제약은 살아있는(deleted_at IS NULL) 행에만 적용한다(partial index).
-- 소프트 삭제된 행이 남아도 같은 코스를 다시 저장할 수 있어야 하기 때문 — 전체 유니크면 재저장이 23505 로 막힌다.
CREATE UNIQUE INDEX uq_saved_courses_user_course
    ON saved_courses (user_id, course_id)
    WHERE deleted_at IS NULL;

-- 2) saved_places
-- 아직 저장 장소 영속 어댑터·쿼리가 없어(모킹 단계) 컬럼만 미리 둔다 — 구현 시 deleted_at IS NULL 가드로 조회한다.
ALTER TABLE saved_places
    ADD COLUMN deleted_at TIMESTAMPTZ;
