-- 코스 따라가기 컨트롤러 통합 테스트 픽스처(course-like-fixture 미러).
-- RESTART IDENTITY 로 serial id 를 고정한다(users 1·2, courses 1~3).
TRUNCATE TABLE users, courses, tracing_courses, tracing_course_places
    RESTART IDENTITY CASCADE;

-- 사용자: 1 = 따라가기 주체(JWT subject), 2 = 타인.
INSERT INTO users (nickname) VALUES ('따라가기주체'), ('타인');

-- 코스: 1 = 공개(성공 대상, tracings_cnt 0), 2 = 타인 비공개(404 대상), 3 = 소프트 삭제(404 대상).
INSERT INTO courses (user_id, title, is_published, visibility)
VALUES
    (2, '코스1', true, 'PUBLIC'),
    (2, '비공개코스2', true, 'PRIVATE');
INSERT INTO courses (user_id, title, is_published, visibility, deleted_at)
VALUES
    (2, '삭제된코스3', true, 'PUBLIC', now());
