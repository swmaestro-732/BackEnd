-- 따라가기 진행 상태 — 완주를 "행 존재"에서 "완주 시각"으로 승격한다.
-- tracing_courses 행 = 따라가기 시작(진행중), completed_at IS NOT NULL = 완주.
ALTER TABLE tracing_courses ADD COLUMN completed_at TIMESTAMPTZ;

-- 같은 tracing 안에서 같은 장소를 두 번 체크인해도 한 번으로 수렴한다(멱등 체크인 · insertIgnore 짝).
ALTER TABLE added_places ADD CONSTRAINT uq_added_places_tracing_place UNIQUE (tracing_course_id, place_id);
