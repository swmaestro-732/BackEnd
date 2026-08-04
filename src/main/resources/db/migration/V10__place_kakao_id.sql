ALTER TABLE places ADD COLUMN kakao_place_id VARCHAR(64);
-- 유니크 인덱스는 아직 걸지 않는다(후속). 그때까지 dedup 은 앱 레벨(findByKakaoIds 조회) 로만 보장한다.
-- 추가 시:
--   CREATE UNIQUE INDEX ux_places_kakao_id ON places (kakao_place_id)
--     WHERE kakao_place_id IS NOT NULL AND deleted_at IS NULL;
