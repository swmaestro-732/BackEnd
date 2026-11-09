-- 어드민 코스 관리와 숨김 코스 노출 점검 통합 테스트 픽스처(SCRUM-752).
-- RESTART IDENTITY 로 id 를 고정한다(users 1~3, places 1~2, courses 1~5).
TRUNCATE TABLE users, places, courses, course_places, course_place_images, course_tags, tags, course_categories,
    admins, admin_actions, notifications, notification_actors RESTART IDENTITY CASCADE;

INSERT INTO admins (email, google_sub) VALUES ('admin@example.com', 'admin-google-sub');

-- 사용자: 1 = 일반 작성자, 2 = 큐레이터(admin.curator-user-id 기본값), 3 = 타인.
-- 발행 개수 캐시: 1 은 course 1, 2 는 course 2 만 잡힌다(숨김 course 4 와 삭제 course 5 는 제외).
INSERT INTO users (nickname, public_courses_cnt, follower_courses_cnt, private_courses_cnt)
VALUES ('일반작성자', 1, 0, 0), ('큐레이터', 1, 0, 0), ('타인', 0, 0, 0);

INSERT INTO places (name, category, location, address)
VALUES
    ('카페A', 'CAFE', 'SRID=4326;POINT(127.05 37.54)'::geography, '서울 성수동 1'),
    ('카페B', 'CAFE', 'SRID=4326;POINT(127.06 37.55)'::geography, '서울 성수동 2');

-- course 1: 일반 작성자의 공개 발행 코스(성수).
INSERT INTO courses (user_id, title, cover_image_url, is_published, visibility, area, area_code, likes_cnt, saves_cnt)
VALUES (1, '사용자 공개 코스', 'https://img/user-cover.jpg', true, 'PUBLIC', '성수', '1120011400', 3, 2);
-- course 2: 큐레이터 공개 발행 코스(편집 대상).
INSERT INTO courses (user_id, title, description, cover_image_url, is_published, visibility, updated_at)
VALUES (2, '큐레이터 공개 코스', '큐레이터 설명', 'https://img/curator-cover.jpg', true, 'PUBLIC', '2026-11-01T00:00:00Z');
-- course 3: 일반 작성자의 임시저장 코스.
INSERT INTO courses (user_id, title, is_published, visibility) VALUES (1, '사용자 초안', false, 'PRIVATE');
-- course 4: 숨긴 큐레이터 발행 코스.
INSERT INTO courses (user_id, title, cover_image_url, is_published, visibility, status)
VALUES (2, '숨긴 큐레이터 코스', 'https://img/hidden-cover.jpg', true, 'PUBLIC', 'HIDDEN');
-- course 5: 삭제된 일반 작성자 발행 코스.
INSERT INTO courses (user_id, title, cover_image_url, is_published, visibility, status, deleted_at)
VALUES (1, '삭제된 코스', 'https://img/deleted-cover.jpg', true, 'PUBLIC', 'DELETED', now());

INSERT INTO course_categories (course_id, category)
VALUES (1, 'CAFETOUR'), (2, 'DATE'), (2, 'CAFETOUR'), (4, 'CAFETOUR');

-- course 1, 2, 4 의 장소 2곳과 장소별 사진 1장.
INSERT INTO course_places (course_id, place_id, order_no, caption, walking_minutes)
VALUES
    (1, 1, 0, '첫 카페', 5), (1, 2, 1, '둘째 카페', NULL),
    (2, 1, 0, '큐레이터 첫 카페', 7), (2, 2, 1, '큐레이터 둘째 카페', NULL),
    (4, 1, 0, '숨김 첫 카페', 4), (4, 2, 1, '숨김 둘째 카페', NULL);

INSERT INTO course_place_images (course_place_id, image_url, order_no)
VALUES
    (1, 'https://img/p1.jpg', 0), (2, 'https://img/p2.jpg', 0),
    (3, 'https://img/p3.jpg', 0), (4, 'https://img/p4.jpg', 0),
    (5, 'https://img/p5.jpg', 0), (6, 'https://img/p6.jpg', 0);

INSERT INTO tags (name) VALUES ('성수');
INSERT INTO course_tags (course_id, tag_id) VALUES (2, 1);
