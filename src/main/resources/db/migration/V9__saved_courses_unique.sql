-- 코스 저장 중복 방지: (user_id, course_id) 유니크 인덱스.
-- 서비스의 사전검사(existsSavedCourse)와 별개로, 동시 저장 경합(TOCTOU)에서도
-- 코스당 저장 레코드가 1개임을 DB 가 보장한다.
-- 위반(SQLSTATE 23505)은 GlobalExceptionHandler 가 인덱스명(saved_courses)으로 식별해
-- 4093(COURSE_ALREADY_SAVED)로 변환한다. user_id 는 V6 에서 추가됐다.
CREATE UNIQUE INDEX uq_saved_courses_user_course
    ON saved_courses (user_id, course_id);
