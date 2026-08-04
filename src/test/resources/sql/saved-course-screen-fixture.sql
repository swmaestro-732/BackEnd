-- 저장함 · 코스 탭 화면 조합(BFF) 통합 테스트 픽스처.
-- RESTART IDENTITY 로 serial id 를 예측 가능하게 고정한다
-- (users 1~3, places 1~4, courses 1·2, course_places 1~4, folders 1·2, saved_courses 1·2).
TRUNCATE TABLE users, places, courses, course_places, course_place_images,
    saved_course_folders, saved_courses, tracing_courses
    RESTART IDENTITY CASCADE;

-- 사용자: 1 = 저장 주체(dev, 로그인 사용자), 2·3 = 코스 작성자
INSERT INTO users (nickname, handle, profile_image_url) VALUES
    ('현우',       'hyunwoo',     'https://img/hyunwoo.jpg'),
    ('지호',       'jiho_routes', 'https://img/jiho.jpg'),
    ('슬로우서울', 'slow_seoul',  'https://img/seoul.jpg');

-- 장소: 코스 장소 핀·상세(이름·카테고리·주소·좌표·이미지). location 은 NOT NULL.
INSERT INTO places (name, category, location, address, image_url) VALUES
    ('어니언 성수',   'CAFE',     'SRID=4326;POINT(127.0578 37.5445)'::geography, '서울 성동구 성수동 A', 'https://img/place-onion.jpg'),
    ('센터커피 성수', 'CAFE',     'SRID=4326;POINT(127.0537 37.5463)'::geography, '서울 성동구 성수동 B', 'https://img/place-center.jpg'),
    ('리움미술관',    'CULTURE',  'SRID=4326;POINT(127.0000 37.5385)'::geography, '서울 용산구 한남동 C', 'https://img/place-leeum.jpg'),
    ('블루스퀘어',    'LANDMARK', 'SRID=4326;POINT(127.0086 37.5326)'::geography, '서울 용산구 한남동 D', 'https://img/place-blue.jpg');

-- course 1: 성수 카페 코스(작성자 2 지호, 공개). course 2: 한남 갤러리 코스(작성자 1 dev = 내 코스, 공개).
INSERT INTO courses (user_id, title, area, category, cover_image_url, is_published, visibility) VALUES
    (2, '성수 카페 코스',    '성수', 'CAFETOUR', 'https://img/cover-cafe.jpg',    true, 'PUBLIC'),
    (1, '한남 갤러리 코스',  '한남', 'CULTURE',  'https://img/cover-gallery.jpg', true, 'PUBLIC');

-- 코스 장소(order_no·caption·도보시간). course_places id: 1·2 = course1, 3·4 = course2.
INSERT INTO course_places (course_id, place_id, order_no, caption, walking_minutes) VALUES
    (1, 1, 0, '통창 자리',      6),
    (1, 2, 1, '핸드드립',       NULL),
    (2, 3, 0, '리움에서 시작',  10),
    (2, 4, 1, '공연장 앞',      NULL);

-- 코스 사진(course_place_images) — 장소 자체 이미지가 아니라 작성자가 코스에 올린 사진. 첫 장소는 2장.
INSERT INTO course_place_images (course_place_id, image_url, order_no) VALUES
    (1, 'https://img/c1p0-a.jpg', 0),
    (1, 'https://img/c1p0-b.jpg', 1),
    (2, 'https://img/c1p1-a.jpg', 0),
    (3, 'https://img/c2p0-a.jpg', 0),
    (4, 'https://img/c2p1-a.jpg', 0);

-- 저장 폴더(user 1): 1 = 데이트, 2 = 혼자 걷기.
INSERT INTO saved_course_folders (user_id, name, order_no) VALUES
    (1, '데이트',     0),
    (1, '혼자 걷기',  1);

-- 저장 레코드(user 1): id 1 = 성수(folder 데이트), id 2 = 한남(folder 혼자 걷기).
-- id 내림차순(최신 저장순)이라 목록은 [한남(2), 성수(1)] 순.
INSERT INTO saved_courses (user_id, folder_id, course_id, created_at) VALUES
    (1, 1, 1, '2026-03-10T00:00:00Z'),
    (1, 2, 2, '2026-07-15T00:00:00Z');

-- 완주(따라가기): user 1 이 한남 갤러리 코스(2)를 완주.
INSERT INTO tracing_courses (user_id, course_id, created_at) VALUES (1, 2, '2026-07-20T00:00:00Z');
