-- 코스 CRUD 컨트롤러 통합 테스트 픽스처.
-- RESTART IDENTITY 로 serial id 를 예측 가능하게 고정한다(users 1·2, places 1·2, courses 1·2·3·4).
TRUNCATE TABLE users, places, courses, course_places, course_place_images, course_tags, tags
    RESTART IDENTITY CASCADE;

-- 사용자: 1 = 코스 소유자, 2 = 타인
-- 소유자(1)의 발행 코스 개수 캐시: 아래 course 1(PUBLIC·발행)만 잡혀 public=1(course 2 는 임시저장이라 제외).
-- 코스 생성/발행/삭제 시 이 캐시가 ±1 로 유지되는지 CourseCountMaintenanceTest 가 검증한다.
INSERT INTO users (nickname, public_courses_cnt, follower_courses_cnt, private_courses_cnt)
VALUES ('코스작성자', 1, 0, 0), ('타인', 1, 0, 0);

-- 장소: 발행 코스 카테고리 도출 입력(CAFE → CAFETOUR). location 은 NOT NULL 이라 임의 좌표를 넣는다.
INSERT INTO places (name, category, location, address)
VALUES
    ('카페A', 'CAFE', 'SRID=4326;POINT(127.05 37.54)'::geography, '서울 성수동 1'),
    ('카페B', 'CAFE', 'SRID=4326;POINT(127.06 37.55)'::geography, '서울 성수동 2');

-- course 1: 공개(PUBLIC)·발행 코스, 소유자 1. 상세/삭제 테스트 대상.
INSERT INTO courses (user_id, title, description, cover_image_url, category, is_published, visibility, tracings_cnt)
VALUES (1, '공개 발행 코스', '설명입니다', 'https://img/cover.jpg', 'CAFETOUR', true, 'PUBLIC', 1200);

-- course 2: 비공개(PRIVATE)·임시저장 코스, 소유자 1. 공개범위/편집 및 초안 목록 테스트 대상(수정일이 더 오래됨).
INSERT INTO courses (user_id, title, is_published, visibility, created_at, updated_at)
VALUES (1, '비공개 초안', false, 'PRIVATE', '2026-07-01T00:00:00Z', '2026-08-01T00:00:00Z');

-- course 3: 타인의 임시저장 코스. 소유권 검증(편집·삭제 404)과 작성자별 초안 격리 테스트 대상.
INSERT INTO courses (user_id, title, is_published, visibility, created_at, updated_at)
VALUES (2, '남의 초안', false, 'PUBLIC', '2026-07-03T00:00:00Z', '2026-08-03T00:00:00Z');

-- course 4: 소유자 1의 두 번째 임시저장 코스. course 2보다 최근에 수정되어 목록에서 먼저 나와야 한다.
INSERT INTO courses (user_id, title, is_published, visibility, created_at, updated_at)
VALUES (1, '최근 수정 초안', false, 'PRIVATE', '2026-07-02T00:00:00Z', '2026-08-02T00:00:00Z');

-- course 1 의 장소 2곳(order_no 0·1)과 각 장소 사진 1장.
INSERT INTO course_places (course_id, place_id, order_no, caption, walking_minutes)
VALUES
    (1, 1, 0, '카페A', 5),
    (1, 2, 1, '카페B', NULL);

INSERT INTO course_place_images (course_place_id, image_url, order_no)
VALUES
    (1, 'https://img/place-a.jpg', 0),
    (2, 'https://img/place-b.jpg', 0);

-- course 1 의 해시태그 2개. 응답 순서는 보장하지 않으므로(course_tags 에 순서 컬럼 없음)
-- 테스트도 순서를 검증하지 않는다. course 2·3·4 는 태그가 없어 응답에서 빈 배열이 된다.
INSERT INTO tags (name) VALUES ('데이트'), ('감성카페');
INSERT INTO course_tags (course_id, tag_id) VALUES (1, 1), (1, 2);
