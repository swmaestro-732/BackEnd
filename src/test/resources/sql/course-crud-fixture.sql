-- 코스 CRUD 컨트롤러 통합 테스트 픽스처.
-- RESTART IDENTITY 로 serial id 를 예측 가능하게 고정한다(users 1·2, places 1·2, courses 1·2·3).
TRUNCATE TABLE users, places, courses, course_places, course_place_images, course_tags, tags
    RESTART IDENTITY CASCADE;

-- 사용자: 1 = 코스 소유자, 2 = 타인
INSERT INTO users (nickname) VALUES ('코스작성자'), ('타인');

-- 장소: 발행 코스 카테고리 도출 입력(CAFE → CAFETOUR). location 은 NOT NULL 이라 임의 좌표를 넣는다.
INSERT INTO places (name, category, location, address)
VALUES
    ('카페A', 'CAFE', 'SRID=4326;POINT(127.05 37.54)'::geography, '서울 성수동 1'),
    ('카페B', 'CAFE', 'SRID=4326;POINT(127.06 37.55)'::geography, '서울 성수동 2');

-- course 1: 공개(PUBLIC)·발행 코스, 소유자 1. 상세/삭제 테스트 대상.
INSERT INTO courses (user_id, title, description, cover_image_url, category, is_published, visibility, tracings_cnt)
VALUES (1, '공개 발행 코스', '설명입니다', 'https://img/cover.jpg', 'CAFETOUR', true, 'PUBLIC', 1200);

-- course 2: 비공개(PRIVATE)·임시저장 코스, 소유자 1. 공개범위/편집 테스트 대상.
INSERT INTO courses (user_id, title, is_published, visibility)
VALUES (1, '비공개 초안', false, 'PRIVATE');

-- course 3: 공개·발행 코스, 소유자 2(타인). 소유권 검증(편집·삭제 404) 테스트 대상.
INSERT INTO courses (user_id, title, cover_image_url, category, is_published, visibility)
VALUES (2, '남의 코스', 'https://img/other.jpg', 'CAFETOUR', true, 'PUBLIC');

-- course 1 의 장소 2곳(order_no 0·1)과 각 장소 사진 1장.
INSERT INTO course_places (course_id, place_id, order_no, caption, walking_minutes)
VALUES
    (1, 1, 0, '카페A', 5),
    (1, 2, 1, '카페B', NULL);

INSERT INTO course_place_images (course_place_id, image_url, order_no)
VALUES
    (1, 'https://img/place-a.jpg', 0),
    (2, 'https://img/place-b.jpg', 0);
