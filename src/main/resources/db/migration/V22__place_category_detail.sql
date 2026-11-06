-- 장소 상세 분류·카카오 분류 원문 (SCRUM-763).

-- 매칭된 카카오 분류 규칙의 마지막 단계 한글 원문(예: '수목원,식물원', '편의점', '음식점'). 클라이언트 아이콘 매핑용.
-- 기존 장소, 그룹 코드로만 분류된 장소, ETC 는 NULL.
ALTER TABLE public.places ADD COLUMN category_detail VARCHAR(50);

-- 카카오 category_name 원문 전체(예: '여행 > 관광,명소 > 수목원,식물원'). 매핑 규칙 변경 시 카카오 재호출 없이 재분류하기 위해 보관한다.
ALTER TABLE public.places ADD COLUMN kakao_category_name VARCHAR(255);
