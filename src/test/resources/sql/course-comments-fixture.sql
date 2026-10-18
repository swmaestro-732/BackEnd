-- 댓글 컨트롤러 통합 테스트: 코스 1의 활성 댓글 3개, 삭제 댓글 1개, 다른 코스 댓글 2개.
TRUNCATE TABLE users, courses, course_comments RESTART IDENTITY CASCADE;

INSERT INTO users (nickname) VALUES ('댓글작성자'), ('다른작성자');

INSERT INTO courses (user_id, title, is_published, visibility, comments_cnt)
VALUES
    (1, '댓글이 있는 코스', true, 'PUBLIC', 3),
    (2, '다른 코스', true, 'PUBLIC', 1),
    (1, '삭제된 코스', true, 'PUBLIC', 1);

UPDATE courses SET status = 'DELETED', deleted_at = '2026-09-02T00:00:00Z' WHERE id = 3;

INSERT INTO course_comments (course_id, user_id, content, created_at, updated_at)
VALUES
    (1, 1, '첫 댓글', '2026-09-01T00:00:00Z', '2026-09-01T00:00:00Z'),
    (1, 2, '다른 작성자 댓글', '2026-09-01T01:00:00Z', '2026-09-01T01:00:00Z'),
    (1, 1, '최신 댓글', '2026-09-01T02:00:00Z', '2026-09-01T02:00:00Z'),
    (1, 1, '이미 삭제된 댓글', '2026-09-01T03:00:00Z', '2026-09-02T00:00:00Z'),
    (2, 1, '다른 코스의 댓글', '2026-09-01T04:00:00Z', '2026-09-01T04:00:00Z'),
    (3, 1, '삭제된 코스의 댓글', '2026-09-01T05:00:00Z', '2026-09-01T05:00:00Z');

UPDATE course_comments SET status = 'DELETED', deleted_at = '2026-09-02T00:00:00Z' WHERE id = 4;
