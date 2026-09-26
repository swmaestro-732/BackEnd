-- 기존 참조와 FK를 보존하며 코스 복제 명칭으로 통일한다.
-- 구 버전 앱은 forked_from_id를 사용하므로 구·신 버전 동시 운영 없이 전환해야 한다.
ALTER TABLE courses RENAME COLUMN forked_from_id TO duplicated_from_id;
ALTER TABLE courses RENAME CONSTRAINT courses_forked_from_id_fkey TO courses_duplicated_from_id_fkey;

-- 복제 당시의 서로 다른 place_id 개수. 과거 기록은 당시 값을 복원할 수 없어 NULL로 남긴다.
ALTER TABLE courses
    ADD COLUMN original_place_count integer,
    ADD COLUMN shared_place_count integer,
    ADD CONSTRAINT courses_duplication_snapshot_check CHECK (
        (original_place_count IS NULL AND shared_place_count IS NULL)
        OR (
            duplicated_from_id IS NOT NULL
            AND original_place_count IS NOT NULL
            AND shared_place_count IS NOT NULL
            AND shared_place_count >= 2
            AND original_place_count >= shared_place_count
        )
    );

COMMENT ON COLUMN courses.duplicated_from_id IS '직접 복제한 원본 코스 ID. 일반 코스는 NULL';
COMMENT ON COLUMN courses.original_place_count IS '복제 당시 원본의 서로 다른 장소 수. 일반 코스·과거 복제 기록은 NULL';
COMMENT ON COLUMN courses.shared_place_count IS '복제 당시 원본과 겹치는 서로 다른 장소 수. 편집 시 보존';
