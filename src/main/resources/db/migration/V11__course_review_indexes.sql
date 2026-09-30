-- 코스 후기 전체보기 화면의 조회 인덱스 (SCRUM-499).
-- 일반 CREATE INDEX는 생성 중 쓰기를 막으므로 배포 시 실행 시간과 잠금을 확인한다.

-- findReviewsByCourse(최신순)·countReviewsByRating·countPhotosByCourse:
-- course_id로 살아있는(공개·미삭제) 리뷰 범위를 좁히고 created_at DESC, id DESC 키셋 정렬을 지원한다.
-- 조건은 CourseReviewQueryRepository.alive() 와 같아야 플래너가 채택한다.
CREATE INDEX idx_course_reviews_course_latest
    ON public.course_reviews (course_id, created_at DESC, id DESC)
    WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

-- findPhotoUrls·countPhotosByCourse: FK 컬럼 인덱스. order_no까지 넣어 리뷰별 사진 순서를 인덱스 순서로 받는다.
CREATE INDEX idx_course_review_photos_review
    ON public.course_review_photos (course_review_id, order_no);
