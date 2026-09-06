-- 리뷰 실구현(SCRUM-487) 스키마 통합 — 장소·코스 리뷰 태그의 코드(enum) 전환 + 별점 비정규화 카운터.
--
-- [태그] place_review_tags·course_review_tags 는 (id, label, icon) 마스터였지만 시드가 없어 한 번도 채워진 적이 없다.
-- 문구·이모지는 화면 표기라 코드(PlaceReviewTag·CourseReviewTag enum)가 들고, 서버가 저장할 것은 태그 코드뿐이다.
-- 그래서 마스터를 없애고 링크 테이블에 **enum 이름**(대문자, 예: COFFEE / PACKED)을 그대로 저장한다 —
-- places.category(PlaceCategory)·리뷰 status 와 같은 "enum 이름이 DB 저장 계약" 규칙이다.
-- API 계약의 태그 코드(소문자 coffee / packed)는 enum 이름의 소문자 표기다.
-- 관련 테이블이 전부 비어 있어(리뷰 작성이 지금까지 모킹) 데이터 이전 없이 컬럼을 교체한다.
--
-- [카운터] 상세·후기 화면이 리뷰 테이블 집계 없이 평균을 그리기 위한 캐시.
-- courses.saves_cnt 와 같은 상대 갱신(±) 규칙: 리뷰 작성 +rating/+1, 소프트 삭제 -rating/-1.
-- 평균은 rating_sum / rating_cnt 로 계산한다(rating_cnt = 0 이면 리뷰 없음).

-- 1) 장소 리뷰 태그 → 코드(enum)
ALTER TABLE public.place_review_tag_links
    DROP CONSTRAINT place_review_tag_links_place_review_tag_id_fkey,
    DROP CONSTRAINT place_review_tag_links_pkey,
    DROP COLUMN place_review_tag_id,
    ADD COLUMN tag character varying(32) NOT NULL,
    ADD CONSTRAINT place_review_tag_links_pkey PRIMARY KEY (place_review_id, tag);

DROP TABLE public.place_review_tags;

-- 2) 코스 리뷰 태그 → 코드(enum)
ALTER TABLE public.course_review_tag_links
    DROP CONSTRAINT course_review_tag_links_course_review_tag_id_fkey,
    DROP CONSTRAINT course_review_tag_links_pkey,
    DROP COLUMN course_review_tag_id,
    ADD COLUMN tag character varying(32) NOT NULL,
    ADD CONSTRAINT course_review_tag_links_pkey PRIMARY KEY (course_review_id, tag);

DROP TABLE public.course_review_tags;

-- 3) 장소 별점 카운터 + 기존 살아있는 리뷰 백필
ALTER TABLE public.places
    ADD COLUMN rating_sum bigint DEFAULT 0 NOT NULL,
    ADD COLUMN rating_cnt integer DEFAULT 0 NOT NULL;

UPDATE public.places p
SET rating_sum = agg.rating_sum,
    rating_cnt = agg.rating_cnt
FROM (
    SELECT place_id, SUM(rating) AS rating_sum, COUNT(*) AS rating_cnt
    FROM public.place_reviews
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL
    GROUP BY place_id
) agg
WHERE p.id = agg.place_id;

-- 4) 코스 별점 카운터 + 백필
ALTER TABLE public.courses
    ADD COLUMN rating_sum bigint DEFAULT 0 NOT NULL,
    ADD COLUMN rating_cnt integer DEFAULT 0 NOT NULL;

UPDATE public.courses c
SET rating_sum = agg.rating_sum,
    rating_cnt = agg.rating_cnt
FROM (
    SELECT course_id, SUM(rating) AS rating_sum, COUNT(*) AS rating_cnt
    FROM public.course_reviews
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL
    GROUP BY course_id
) agg
WHERE c.id = agg.course_id;
