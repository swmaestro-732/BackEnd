-- 기존 참조와 FK를 보존하며 코스 복제 명칭으로 통일한다.
-- 구 버전 앱은 forked_from_id를 사용하므로 구·신 버전 동시 운영 없이 전환해야 한다.
-- (V9 는 계획 기능이 먼저 차지해 V10 으로 올린다.)
ALTER TABLE courses RENAME COLUMN forked_from_id TO duplicated_from_id;
ALTER TABLE courses RENAME CONSTRAINT courses_forked_from_id_fkey TO courses_duplicated_from_id_fkey;

COMMENT ON COLUMN courses.duplicated_from_id IS '직접 복제한 원본 코스 ID. 일반 코스는 NULL';
