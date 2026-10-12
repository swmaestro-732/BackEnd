-- 장소 운영시간 동기화 시각 (SCRUM-544). Google Places 조회를 시도해 결과(있음·없음)를 place_business_hours 에 반영한 시각.
-- NULL 이면 아직 조회 전(또는 조회 실패) → 신규 저장 후 비동기 동기화·부팅 백필의 대상이다.
ALTER TABLE public.places ADD COLUMN business_hours_synced_at TIMESTAMPTZ;
