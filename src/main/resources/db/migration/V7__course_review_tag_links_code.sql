-- 코스 리뷰 작성 실구현(SCRUM-487) — 리뷰 태그를 마스터 테이블이 아니라 **코드(enum) 정본**으로 바꾼다.
--
-- course_review_tags 는 (id, label, icon) 마스터였지만 시드가 없어 한 번도 채워진 적이 없다.
-- 장소 리뷰(V5)와 같은 규칙: 문구·이모지는 화면 표기라 코드(CourseReviewTag enum)가 들고,
-- 링크 테이블에는 **enum 이름**(대문자, 예: PACKED)을 그대로 저장한다.
-- API 계약의 태그 코드(소문자 packed)는 enum 이름의 소문자 표기다.
--
-- 두 테이블 모두 비어 있어(리뷰 작성이 지금까지 모킹) 데이터 이전 없이 컬럼을 교체한다.
ALTER TABLE public.course_review_tag_links
    DROP CONSTRAINT course_review_tag_links_course_review_tag_id_fkey,
    DROP CONSTRAINT course_review_tag_links_pkey,
    DROP COLUMN course_review_tag_id,
    ADD COLUMN tag character varying(32) NOT NULL,
    ADD CONSTRAINT course_review_tag_links_pkey PRIMARY KEY (course_review_id, tag);

DROP TABLE public.course_review_tags;
