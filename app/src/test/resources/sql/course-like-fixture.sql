-- 코스 좋아요 컨트롤러 통합 테스트 픽스처.
-- RESTART IDENTITY 로 serial id 를 예측 가능하게 고정한다(users 1·2, courses 1~3, course_likes 1).
TRUNCATE TABLE users, courses, course_likes
    RESTART IDENTITY CASCADE;

-- 사용자: 1 = 좋아요 주체(JWT subject), 2 = 타인.
INSERT INTO users (nickname) VALUES ('좋아요주체'), ('타인');

-- 코스: 1 = 미좋아요(성공 대상, likes_cnt 0), 2 = 이미 좋아요됨(중복·취소용, likes_cnt 1), 3 = 소프트 삭제(404 대상).
-- status·created_at·카운터는 DB 기본값이 채운다(saved-course-fixture 선례).
INSERT INTO courses (user_id, title, is_published, visibility, likes_cnt)
VALUES
    (1, '코스1', true, 'PUBLIC', 0),
    (1, '코스2', true, 'PUBLIC', 1);
INSERT INTO courses (user_id, title, is_published, visibility, likes_cnt, deleted_at)
VALUES
    (1, '삭제된코스3', true, 'PUBLIC', 0, now());

-- 좋아요 주체(1)가 코스2 를 이미 좋아요한 상태(중복 409·취소 대상).
INSERT INTO course_likes (user_id, course_id)
VALUES (1, 2);
