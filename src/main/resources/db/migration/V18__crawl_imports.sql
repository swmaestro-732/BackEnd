-- 외부 크롤러의 게시물 처리 이력과 중복 수집 방지용 조회 뷰 (SCRUM-694).
-- 결과 기록은 Backend가 SQS 수신 후 담당한다. 코스가 삭제돼도 처리 이력은 남긴다.
CREATE TABLE public.crawl_imports (
    id bigserial PRIMARY KEY,
    shortcode varchar(64) NOT NULL UNIQUE,
    run_id varchar(64) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('IMPORTED', 'REJECTED', 'FAILED')),
    course_id bigint REFERENCES public.courses(id) ON DELETE SET NULL,
    reason text,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL
);

-- 유니크 인덱스 전에 기존 활성 중복을 정리한다. prod 기준 19쌍(2026-10-25 검색 경합으로 생성), 참조는 course_places 2행뿐이었다.
-- 같은 kakao_place_id 중 가장 오래된(id 최소) 장소를 남기고, 나머지를 가리키는 행을 남길 장소로 옮긴 뒤 소프트 삭제한다.
CREATE TEMP TABLE place_dedup ON COMMIT DROP AS
SELECT p.id AS dup_id, k.keep_id
FROM public.places p
JOIN (
    SELECT kakao_place_id, min(id) AS keep_id
    FROM public.places
    WHERE deleted_at IS NULL AND kakao_place_id IS NOT NULL
    GROUP BY kakao_place_id
    HAVING count(*) > 1
) k ON k.kakao_place_id = p.kakao_place_id
WHERE p.deleted_at IS NULL AND p.id <> k.keep_id;

-- 옮기면 유니크 제약과 부딪히는 행은 먼저 치운다(남길 장소에 같은 요일·같은 사용자·같은 따라가기 행이 이미 있는 경우).
DELETE FROM public.place_business_hours h USING place_dedup d
WHERE h.place_id = d.dup_id
  AND EXISTS (SELECT 1 FROM public.place_business_hours k WHERE k.place_id = d.keep_id AND k.day_of_week = h.day_of_week);
DELETE FROM public.tracing_course_places t USING place_dedup d
WHERE t.place_id = d.dup_id
  AND EXISTS (SELECT 1 FROM public.tracing_course_places k WHERE k.place_id = d.keep_id AND k.tracing_course_id = t.tracing_course_id);
UPDATE public.saved_places s SET deleted_at = now() FROM place_dedup d
WHERE s.place_id = d.dup_id AND s.deleted_at IS NULL
  AND EXISTS (SELECT 1 FROM public.saved_places k WHERE k.place_id = d.keep_id AND k.user_id = s.user_id AND k.deleted_at IS NULL);
UPDATE public.place_reviews r SET deleted_at = now() FROM place_dedup d
WHERE r.place_id = d.dup_id AND r.deleted_at IS NULL
  AND EXISTS (SELECT 1 FROM public.place_reviews k WHERE k.place_id = d.keep_id AND k.user_id = r.user_id AND k.deleted_at IS NULL);

UPDATE public.course_places t SET place_id = d.keep_id FROM place_dedup d WHERE t.place_id = d.dup_id;
UPDATE public.plan_places t SET place_id = d.keep_id FROM place_dedup d WHERE t.place_id = d.dup_id;
UPDATE public.added_places t SET place_id = d.keep_id FROM place_dedup d WHERE t.place_id = d.dup_id;
UPDATE public.saved_places t SET place_id = d.keep_id FROM place_dedup d WHERE t.place_id = d.dup_id;
UPDATE public.place_reviews t SET place_id = d.keep_id FROM place_dedup d WHERE t.place_id = d.dup_id;
UPDATE public.place_business_hours t SET place_id = d.keep_id FROM place_dedup d WHERE t.place_id = d.dup_id;
UPDATE public.tracing_course_places t SET place_id = d.keep_id FROM place_dedup d WHERE t.place_id = d.dup_id;

UPDATE public.places p SET deleted_at = now(), updated_at = now() FROM place_dedup d WHERE p.id = d.dup_id;

-- 활성 장소만 카카오 ID로 중복 방지한다. 소프트 삭제된 장소는 다시 수집할 수 있다.
CREATE UNIQUE INDEX uq_places_kakao_place_id
    ON public.places (kakao_place_id)
    WHERE deleted_at IS NULL AND kakao_place_id IS NOT NULL;

-- FAILED는 재시도 대상이므로 이미 처리한 게시물 목록에서 제외한다.
CREATE VIEW public.crawler_known_posts AS
SELECT shortcode
FROM public.crawl_imports
WHERE status IN ('IMPORTED', 'REJECTED');

-- 이미 저장된 장소도 이미지가 없으면 크롤러가 보완할 수 있도록 여부를 함께 노출한다.
CREATE VIEW public.crawler_known_places AS
SELECT kakao_place_id, image_url IS NOT NULL AND image_url <> '' AS has_image
FROM public.places
WHERE deleted_at IS NULL AND kakao_place_id IS NOT NULL;

-- 역할은 클러스터 공통이므로 다른 DB에서 이미 생성했으면 재사용한다.
-- LOGIN + PASSWORD는 SCRUM-693 인프라 프로비저닝에서 Secrets Manager 값으로 설정한다.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'crawler_ro') THEN
        CREATE ROLE crawler_ro NOLOGIN;
    END IF;
END;
$$;

-- 원본 테이블 접근이나 쓰기 권한 없이 두 뷰만 읽는다.
GRANT USAGE ON SCHEMA public TO crawler_ro;
GRANT SELECT ON public.crawler_known_posts, public.crawler_known_places TO crawler_ro;

-- 크롤러 SG 는 RDS SG 를 writer 와 함께 쓰므로 네트워크상 writer 에도 닿는다. 권한(뷰 SELECT)에 더해
-- 세션 기본값도 읽기 전용으로 고정해 실수로 쓰기 트랜잭션을 열지 못하게 한다.
ALTER ROLE crawler_ro SET default_transaction_read_only = on;
