-- 계획 상세 화면 조합(BFF) 통합 테스트 픽스처.
-- 계획 1(소유자 1)은 장소 3곳을 담되 그중 하나(place 3)는 소프트 삭제된 장소라 요약이 빠지는 경우를 만든다.
-- 계획 2는 타인(2) 소유 — 소유권 은닉(404) 검증용.
TRUNCATE TABLE plan_places, plans, courses, places, users RESTART IDENTITY CASCADE;

INSERT INTO users (nickname) VALUES ('계획소유자'), ('타인');

INSERT INTO places (name, category, location, address, image_url, deleted_at)
VALUES
    ('어니언 성수', 'CAFE', 'SRID=4326;POINT(127.0575 37.5445)'::geography, '서울 성동구 아차산로9길 8', 'https://cdn/p1.jpg', NULL),
    ('대림창고 갤러리', 'CAFE', 'SRID=4326;POINT(127.0555 37.5419)'::geography, '서울 성동구 성수이로 78', NULL, NULL),
    ('사라진 카페', 'CAFE', 'SRID=4326;POINT(127.0600 37.5500)'::geography, '서울 성동구 어딘가', NULL, '2026-09-01T00:00:00Z');

INSERT INTO plans (id, user_id, title, memo, planned_date, source_course_id, created_at, updated_at)
VALUES
    (1, 1, '토요일 성수 데이트', '3시 전엔 출발', '2026-09-20', NULL, '2026-09-18T07:00:00Z', '2026-09-18T08:00:00Z'),
    (2, 2, '타인 계획', NULL, NULL, NULL, '2026-09-18T07:00:00Z', '2026-09-18T08:00:00Z');

-- 도보 시간: 1구간 6분, 2구간은 도보 불가(-1), 마지막 장소는 NULL → 화면 합계는 6분.
INSERT INTO plan_places (plan_id, place_id, order_no, memo, walking_minutes)
VALUES
    (1, 1, 0, '웨이팅 있으면 옆집으로', 6),
    (1, 2, 1, NULL, -1),
    (1, 3, 2, '삭제된 장소', NULL),
    (2, 1, 0, NULL, NULL);
