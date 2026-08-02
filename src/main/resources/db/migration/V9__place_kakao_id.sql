ALTER TABLE places ADD COLUMN kakao_place_id VARCHAR(64);
CREATE UNIQUE INDEX ux_places_kakao_id ON places (kakao_place_id) WHERE kakao_place_id IS NOT NULL AND deleted_at IS NULL;
