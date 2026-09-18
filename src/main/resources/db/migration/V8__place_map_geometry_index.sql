-- 지도 뷰포트는 위·경도 사각형이므로 geometry 비교를 사용한다.
-- 기존 geography GiST는 location::geometry 표현식에 적용되지 않아 별도 인덱스가 필요하다.
-- V5~V7은 develop의 코스 이벤트·리뷰·앱 버전 정책 마이그레이션이 사용한다.
CREATE INDEX idx_places_map_geometry ON places USING gist ((location::geometry))
    WHERE deleted_at IS NULL AND status = 'ACTIVE';
