-- 코스 별점 비정규화 카운터(SCRUM-487) — 후기 화면이 리뷰 테이블 집계 없이 평균을 그리기 위한 캐시(places V6 과 동일 규칙).
-- courses.saves_cnt 와 같은 상대 갱신(±): 리뷰 작성 +rating/+1, 소프트 삭제 -rating/-1.
-- 평균은 rating_sum / rating_cnt 로 계산한다(rating_cnt = 0 이면 리뷰 없음).
ALTER TABLE public.courses
    ADD COLUMN rating_sum bigint DEFAULT 0 NOT NULL,
    ADD COLUMN rating_cnt integer DEFAULT 0 NOT NULL;

-- 기존 살아있는 리뷰 백필(작성이 모킹이었어 보통 0건이지만, 수동 삽입 데이터 대비)
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
