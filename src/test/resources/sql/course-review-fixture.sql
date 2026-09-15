-- 코스 리뷰 작성·삭제 통합 테스트 픽스처.
-- 매 테스트 메서드마다 재실행되므로 TRUNCATE 로 멱등하게 만든다(id 명시 — 테스트 대상으로 참조).
-- RESTART IDENTITY 로 course_reviews 시퀀스도 되돌려, 첫 작성 리뷰가 항상 id=1 이 되게 한다.
TRUNCATE TABLE course_reviews RESTART IDENTITY CASCADE;
TRUNCATE TABLE courses RESTART IDENTITY CASCADE;

INSERT INTO courses (id, user_id, title, is_published, visibility)
VALUES
    (701, 1, '비 오는 날 성수 카페 코스', true, 'PUBLIC');
