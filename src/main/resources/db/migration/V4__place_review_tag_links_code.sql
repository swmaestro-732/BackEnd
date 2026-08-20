-- 장소 리뷰 작성 실구현(SCRUM-487) — 리뷰 태그를 마스터 테이블이 아니라 **코드(enum) 정본**으로 바꾼다.
--
-- place_review_tags 는 (id, label, icon) 마스터였지만 시드가 없어 한 번도 채워진 적이 없다.
-- 문구·이모지는 화면 표기라 클라이언트/코드가 들고 있으면 충분하고, 서버가 저장할 것은 태그 코드뿐이다.
-- 그래서 마스터를 없애고 `.ai/taxonomy.md` 장소 리뷰 태그 64종을 코드의 enum(PlaceReviewTag)으로 관리하며,
-- 링크 테이블에는 **enum 이름**(대문자, 예: COFFEE)을 그대로 저장한다 —
-- places.category(PlaceCategory)·place_reviews.status 와 같은 "enum 이름이 DB 저장 계약" 규칙이다.
-- API 계약의 태그 코드(소문자 coffee)는 enum 이름의 소문자 표기다.
--
-- 두 테이블 모두 비어 있어(리뷰 작성이 지금까지 모킹) 데이터 이전 없이 컬럼을 교체한다.
ALTER TABLE public.place_review_tag_links
    DROP CONSTRAINT place_review_tag_links_place_review_tag_id_fkey,
    DROP CONSTRAINT place_review_tag_links_pkey,
    DROP COLUMN place_review_tag_id,
    ADD COLUMN tag character varying(32) NOT NULL,
    ADD CONSTRAINT place_review_tag_links_pkey PRIMARY KEY (place_review_id, tag);

DROP TABLE public.place_review_tags;

-- 코스 리뷰 태그(course_review_tags)는 아직 마스터 테이블 그대로다 — 코스 리뷰 작성 실구현 때 같은 방식으로 맞춘다.
