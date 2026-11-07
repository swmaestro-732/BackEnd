-- 저장함 · 장소 탭 화면 조합(BFF) 통합 테스트 픽스처.
-- RESTART IDENTITY 로 serial id 를 예측 가능하게 고정한다 (users 1, places 1~5, saved_places 1~6).
-- area 는 TRUNCATE 하지 않는다 — V13 법정동 시드를 그대로 쓰고(지역 이름 해석), 지우면 다른 테스트가 깨진다.
TRUNCATE TABLE users, places, saved_places RESTART IDENTITY CASCADE;

-- 사용자 1 = 저장 주체(로그인 사용자)
INSERT INTO users (nickname, handle) VALUES ('현우', 'hyunwoo');

-- 장소. area_code 는 V13 시드의 실제 법정동코드를 쓴다(1120011400 성수동1가 · 1117013100 한남동).
-- 4번은 area_code 가 없어 지역 이름이 null 로 내려가는 경로를, 5번은 소프트 삭제로 항목 제외 경로를 검증한다.
INSERT INTO places (name, category, location, address, image_url, area_code, deleted_at) VALUES
    ('어니언 성수',   'CAFE',    'SRID=4326;POINT(127.0578 37.5445)'::geography, '서울 성동구 성수동 A', 'https://img/onion.jpg',  '1120011400', NULL),
    ('센터커피 성수', 'CAFE',    'SRID=4326;POINT(127.0537 37.5463)'::geography, '서울 성동구 성수동 B', 'https://img/center.jpg', '1120011400', NULL),
    ('리움미술관',    'CULTURE', 'SRID=4326;POINT(127.0000 37.5385)'::geography, '서울 용산구 한남동 C', 'https://img/leeum.jpg',  '1117013100', NULL),
    ('지역미상 바',   'BAR',     'SRID=4326;POINT(127.0610 37.5437)'::geography, '서울 성동구 어딘가',   NULL,                     NULL,         NULL),
    ('폐업한 카페',   'CAFE',    'SRID=4326;POINT(127.0500 37.5400)'::geography, '서울 성동구 성수동 E', 'https://img/gone.jpg',   '1120011400', now());

-- 저장 레코드(최신 저장순 = id 내림차순).
--  1 방문한 저장 / 2·3 미방문 / 4 카테고리 미분류 / 5 삭제된 장소를 가리키는 저장(카운트엔 들어가지만 항목에선 빠진다)
--  6 소프트 삭제된 저장(어디에도 집계되지 않는다)
INSERT INTO saved_places (user_id, place_id, category, visited, created_at, deleted_at) VALUES
    (1, 1, 'CAFE',    true,  '2026-07-05T02:15:00Z', NULL),
    (1, 2, 'CAFE',    false, '2026-07-08T11:00:00Z', NULL),
    (1, 3, 'CULTURE', false, '2026-07-12T05:30:00Z', NULL),
    (1, 4, NULL,      false, '2026-07-15T13:05:00Z', NULL),
    (1, 5, 'CAFE',    false, '2026-07-17T09:20:00Z', NULL),
    (1, 3, 'CULTURE', false, '2026-07-18T09:20:00Z', now());
