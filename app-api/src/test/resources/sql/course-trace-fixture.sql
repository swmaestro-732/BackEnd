-- 코스 따라가기 컨트롤러 통합 테스트 픽스처(course-like-fixture 미러).
-- RESTART IDENTITY 로 serial id 를 고정한다(users 1·2, courses 1~3).
TRUNCATE TABLE users, courses, course_places, tracing_courses, tracing_course_places, places
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

-- 코스 1 의 장소: 22·23·25 (체크인 허용 대상). place_id 는 cross-domain 이라 FK 없이 임의 값. 24 는 코스에 없는 장소(400 대상).
INSERT INTO course_places (course_id, place_id, order_no)
VALUES (1, 22, 0), (1, 23, 1), (1, 25, 2);

-- 경로 조회(GET /api/v1/traces) 용 장소: 22·23 은 실제 행(좌표 포함), 99 는 없는 장소(404 대상).
INSERT INTO places (id, name, category, location, address)
VALUES
    (22, '어니언 성수', 'CAFE', 'SRID=4326;POINT(127.0559 37.5446)'::geography, '서울 성동구 아차산로 100'),
    (23, '대림창고', 'CAFE', 'SRID=4326;POINT(127.0563 37.5420)'::geography, '서울 성동구 성수이로 78');
