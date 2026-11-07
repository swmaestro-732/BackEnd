-- 코스·장소 후기 전체보기 화면의 조회 인덱스와 통계 카운터 (SCRUM-499).
-- 일반 CREATE INDEX는 생성 중 쓰기를 막으므로 배포 시 실행 시간과 잠금을 확인한다.

-- ───────── 코스 리뷰 ─────────

-- 1) findReviewsByCourse(최신순): course_id로 살아있는(공개·미삭제) 리뷰 범위를 좁히고 created_at 정렬을 지원한다.
-- 내림차순은 같은 인덱스를 거꾸로 읽고, 정렬 키의 id 동점은 플래너가 Incremental Sort로 처리한다
-- (created_at이 마이크로초라 동점이 사실상 없어 비용 차이 없음, 벤치 실측).
-- 조건은 CourseReviewQueryRepository.alive() 와 같아야 플래너가 채택한다.
CREATE INDEX idx_course_reviews_course_latest
    ON public.course_reviews (course_id, created_at)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

-- 2) findPhotoUrls: FK 컬럼 인덱스. 정렬(ORDER BY order_no)은 페이지의 사진 수십 행에 대한 Sort 로 충분해
-- order_no 는 넣지 않는다 (넣어도 계획·버퍼 동일, 크기만 11MB→6.8MB, 벤치 실측).
CREATE INDEX idx_course_review_photos_review
    ON public.course_review_photos (course_review_id);

-- 3) 화면 통계 카운터 — 사진 총개수와 별점별 리뷰 수.
-- 두 집계(사진 JOIN COUNT, GROUP BY rating)는 리뷰가 많은 코스에서 매 요청 코스 전체를 읽어야 해
-- (벤치: 공개 리뷰 2.7만 건 코스에서 36ms·4ms) rating_sum/rating_cnt(V6)와 같은 방식으로 courses 에 카운터를 두고
-- 리뷰 작성 +, 소프트 삭제 - 로 유지한다. 별점별 카운터의 합은 rating_cnt 와 같다.
-- 백필은 두지 않는다 — 적용 시점에 리뷰 데이터가 없다(있는 환경이면 별도 보정 필요).
ALTER TABLE public.courses
    ADD COLUMN review_photo_cnt integer DEFAULT 0 NOT NULL,
    ADD COLUMN rating_1_cnt integer DEFAULT 0 NOT NULL,
    ADD COLUMN rating_2_cnt integer DEFAULT 0 NOT NULL,
    ADD COLUMN rating_3_cnt integer DEFAULT 0 NOT NULL,
    ADD COLUMN rating_4_cnt integer DEFAULT 0 NOT NULL,
    ADD COLUMN rating_5_cnt integer DEFAULT 0 NOT NULL;

-- ───────── 장소 리뷰 (코스와 같은 구성) ─────────

-- 4) findReviewsByPlace(최신순). 조건은 PlaceReviewQueryRepository.alive() 와 같아야 한다.
CREATE INDEX idx_place_reviews_place_latest
    ON public.place_reviews (place_id, created_at)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

-- 5) findPhotoUrls: FK 컬럼 인덱스 (코스와 같은 이유로 order_no 없음).
CREATE INDEX idx_place_review_photos_review
    ON public.place_review_photos (place_review_id);

-- 6) 화면 통계 카운터.
ALTER TABLE public.places
    ADD COLUMN review_photo_cnt integer DEFAULT 0 NOT NULL,
    ADD COLUMN rating_1_cnt integer DEFAULT 0 NOT NULL,
    ADD COLUMN rating_2_cnt integer DEFAULT 0 NOT NULL,
    ADD COLUMN rating_3_cnt integer DEFAULT 0 NOT NULL,
    ADD COLUMN rating_4_cnt integer DEFAULT 0 NOT NULL,
    ADD COLUMN rating_5_cnt integer DEFAULT 0 NOT NULL;
