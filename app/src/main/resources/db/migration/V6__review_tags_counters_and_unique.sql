-- 리뷰 실구현(SCRUM-487) 스키마 — 태그 코드(enum) 전환 + 별점 비정규화 카운터 + 1인 1리뷰 유니크.
-- 원래 V5(태그·카운터) + V6(유니크) 두 파일이었으나, V5 는 다른 브랜치(SCRUM-523)가 먼저 채번해
-- 한 파일로 합쳐 V6 으로 내렸다. 같은 버전 번호를 두 브랜치가 들고 있으면 Flyway 가
-- "Found more than one migration with version N" 으로 기동을 거부한다(V3 주석 참고).
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
--
-- [유니크] 사용자·대상 조합당 살아있는 리뷰 1개 — saved_places(V2)와 같은 partial unique index.
-- 서비스 사전검사(existsActiveReview)와 별개로 동시 작성 경합(TOCTOU)에서도 DB 가 보장한다.
-- 위반(SQLSTATE 23505)은 GlobalExceptionHandler 가 인덱스명으로 식별해
-- 4096(PLACE_REVIEW_ALREADY_EXISTS)·4097(COURSE_REVIEW_ALREADY_EXISTS)로 변환한다.

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

-- 5) 1인 1리뷰 유니크
-- 유니크 제약은 살아있는(deleted_at IS NULL) 행에만 적용한다(partial index).
-- 리뷰는 소프트 삭제만 하므로(status=DELETED + deleted_at) 전체 유니크로 걸면 한 번 쓰고 지운 사용자가
-- 같은 대상에 다시 쓸 수 없다. status='HIDDEN'(운영 숨김)은 deleted_at 이 NULL 이라 슬롯을 계속 차지한다 — 의도한 동작이다.
-- 기존 중복 데이터 정리는 넣지 않는다: 위 [태그] 항목대로 리뷰 작성이 지금까지 모킹이라 두 테이블 모두 비어 있다.
-- 혹시 중복이 있어 인덱스 생성이 23505 로 실패하면, 데이터를 임의로 지우지 말고 수동 정리 후 재실행한다.

CREATE UNIQUE INDEX uq_place_reviews_user_place
    ON public.place_reviews (user_id, place_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_course_reviews_user_course
    ON public.course_reviews (user_id, course_id)
    WHERE deleted_at IS NULL;
