-- 장소 상세 화면(BFF) 통합 테스트 픽스처.
-- 매 테스트 메서드마다 재실행되므로 TRUNCATE 로 멱등하게 만든다(id 명시 — 테스트 조회 대상으로 참조).
TRUNCATE TABLE places RESTART IDENTITY CASCADE;

INSERT INTO places (id, name, category, location, address, image_url)
VALUES
    (501, '어니언 성수', 'CAFE', 'SRID=4326;POINT(127.0559 37.5446)'::geography, '서울 성동구 아차산로 100', 'https://cdn.example.com/places/501/1.jpg');
