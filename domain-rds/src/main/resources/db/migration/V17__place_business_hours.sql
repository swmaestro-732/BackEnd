-- 장소 운영시간 (SCRUM-544).

-- 동기화 시각. Google Places 조회를 시도해 결과(있음·없음)를 place_business_hours 에 반영한 시각.
-- NULL 이면 아직 조회 전이거나 조회에 실패한 것 — 신규 장소 저장 직후와 장소 상세 조회 시 보완 대상이다.
-- 행이 0개여도 시각이 있으면 "구글에 정보가 없음"이 확인된 것이라 다시 조회하지 않는다.
ALTER TABLE public.places ADD COLUMN business_hours_synced_at TIMESTAMPTZ;

-- 브레이크타임. 구글은 쉬는 시간이 있는 날을 period 2개로 내려준다(예: 11:15~15:30, 17:00~21:30).
-- (place_id, day_of_week) UNIQUE 라 하루 한 행만 가능해, 영업은 첫 개점~마지막 폐점으로 담고 그 사이 공백을 여기 담는다.
-- 표본 47곳 중 하루 3구간 이상은 없어 두 칸으로 충분하다. 쉬는 시간이 없으면 둘 다 NULL.
ALTER TABLE public.place_business_hours
    ADD COLUMN break_start TIME,
    ADD COLUMN break_end   TIME;
