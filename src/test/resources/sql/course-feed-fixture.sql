-- 공개 코스 피드(BFF) 통합 테스트 픽스처.
-- RESTART IDENTITY 로 serial id 를 예측 가능하게 고정한다(users 1..5, courses 1..6).
-- created_at 을 명시로 박아 최신순(동률 방지)을 결정적으로 만든다.
TRUNCATE TABLE users, courses, saved_courses RESTART IDENTITY CASCADE;

-- 저장(save)을 흩뿌릴 사용자 5명.
INSERT INTO users (nickname)
VALUES ('작성자'), ('유저2'), ('유저3'), ('유저4'), ('유저5');

-- course 1: PUBLIC 발행, 가장 최신. 저장 1건.
INSERT INTO courses (user_id, title, cover_image_url, category, is_published, visibility, created_at)
VALUES (1, '공개 최신 코스', 'https://img/c1.jpg', 'CAFETOUR', true, 'PUBLIC', '2026-07-30T00:00:00Z');

-- course 2: PUBLIC 발행, 두 번째로 최신이지만 저장 5건 → 피드 1위(저장수 우선).
INSERT INTO courses (user_id, title, cover_image_url, category, is_published, visibility, created_at)
VALUES (1, '공개 인기 코스', 'https://img/c2.jpg', 'CAFETOUR', true, 'PUBLIC', '2026-07-29T00:00:00Z');

-- course 3: PRIVATE 발행 → 피드 제외.
INSERT INTO courses (user_id, title, is_published, visibility, created_at)
VALUES (1, '비공개 코스', true, 'PRIVATE', '2026-07-28T00:00:00Z');

-- course 4: FOLLOWER 발행 → 피드 제외.
INSERT INTO courses (user_id, title, is_published, visibility, created_at)
VALUES (1, '팔로워 공개 코스', true, 'FOLLOWER', '2026-07-27T00:00:00Z');

-- course 5: PUBLIC 이지만 미발행(임시저장) → 피드 제외.
INSERT INTO courses (user_id, title, is_published, visibility, created_at)
VALUES (1, '공개 임시저장 코스', false, 'PUBLIC', '2026-07-26T00:00:00Z');

-- course 6: PUBLIC 발행, 가장 오래됨. 저장 0건 → 피드 최하위.
INSERT INTO courses (user_id, title, cover_image_url, category, is_published, visibility, created_at)
VALUES (1, '공개 오래된 코스', 'https://img/c6.jpg', 'CAFETOUR', true, 'PUBLIC', '2026-07-25T00:00:00Z');

-- 저장 기록: course 2 를 5명이, course 1 을 1명이 저장. course 6 은 저장 없음.
INSERT INTO saved_courses (user_id, course_id, created_at)
VALUES
    (1, 2, '2026-07-29T01:00:00Z'),
    (2, 2, '2026-07-29T02:00:00Z'),
    (3, 2, '2026-07-29T03:00:00Z'),
    (4, 2, '2026-07-29T04:00:00Z'),
    (5, 2, '2026-07-29T05:00:00Z'),
    (1, 1, '2026-07-30T01:00:00Z');
