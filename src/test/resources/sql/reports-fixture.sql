-- 신고 컨트롤러 통합 테스트 픽스처. RESTART IDENTITY 로 id 를 고정한다.
-- users: 1 = 신고자, 2 = 신고 대상 콘텐츠 작성자, 3 = 탈퇴 사용자
-- courses: 1 = 공개(작성자 2), 2 = 비공개(작성자 2, 신고자가 볼 수 없음), 3 = 삭제됨(작성자 2), 4 = 신고자 본인 코스
-- course_comments: 1 = 작성자 2 활성, 2 = 신고자 본인 댓글, 3 = 작성자 2 삭제됨
TRUNCATE TABLE users, courses, course_comments, reports RESTART IDENTITY CASCADE;

INSERT INTO users (nickname) VALUES ('신고자'), ('작성자'), ('탈퇴자');
UPDATE users SET deleted_at = '2026-09-01T00:00:00Z' WHERE id = 3;

INSERT INTO courses (user_id, title, is_published, visibility)
VALUES
    (2, '공개 코스', true, 'PUBLIC'),
    (2, '비공개 코스', true, 'PRIVATE'),
    (2, '삭제된 코스', true, 'PUBLIC'),
    (1, '내 코스', true, 'PUBLIC');
UPDATE courses SET status = 'DELETED', deleted_at = '2026-09-02T00:00:00Z' WHERE id = 3;

INSERT INTO course_comments (course_id, user_id, content)
VALUES
    (1, 2, '작성자 댓글'),
    (1, 1, '내 댓글'),
    (1, 2, '삭제된 댓글');
UPDATE course_comments SET status = 'DELETED', deleted_at = '2026-09-02T00:00:00Z' WHERE id = 3;
