-- 동시성 테스트: 댓글 수 카운터(comments_cnt)가 0인 활성 코스 1개만 시드한다.
TRUNCATE TABLE users, courses, course_comments RESTART IDENTITY CASCADE;

INSERT INTO users (nickname) VALUES ('동시성작성자');

INSERT INTO courses (user_id, title, is_published, visibility, comments_cnt)
VALUES (1, '동시성 테스트 코스', true, 'PUBLIC', 0);
