-- 코스 계획 CRUD 통합 테스트 픽스처.
-- 매 테스트 메서드마다 재실행되므로 TRUNCATE 로 멱등하게 만든다.
-- RESTART IDENTITY 로 plans 시퀀스를 되돌려, 첫 생성 계획이 항상 id=1 이 되게 한다.
TRUNCATE TABLE plan_places, plans, courses, places, users RESTART IDENTITY CASCADE;

-- 사용자: 1 = 계획 소유자, 2 = 타인(소유권 은닉 검증용)
INSERT INTO users (nickname) VALUES ('계획소유자'), ('타인');

-- 장소 1·2·3 — 계획에 담는 대상. location 은 NOT NULL 이라 임의 좌표를 넣는다.
INSERT INTO places (name, category, location, address)
VALUES
    ('카페A', 'CAFE', 'SRID=4326;POINT(127.05 37.54)'::geography, '서울 성수동 1'),
    ('카페B', 'CAFE', 'SRID=4326;POINT(127.06 37.55)'::geography, '서울 성수동 2'),
    ('카페C', 'CAFE', 'SRID=4326;POINT(127.07 37.56)'::geography, '서울 성수동 3');

-- course 901 — 계획 복제 원본(sourceCourseId) 검증용.
INSERT INTO courses (id, user_id, title, is_published, visibility)
VALUES (901, 1, '성수 카페 코스', true, 'PUBLIC');
