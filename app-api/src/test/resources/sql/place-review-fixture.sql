-- 장소 리뷰 작성 통합 테스트 픽스처.
-- 매 테스트 메서드마다 재실행되므로 TRUNCATE 로 멱등하게 만든다(id 명시 — 테스트 대상으로 참조).
-- RESTART IDENTITY 로 place_reviews 시퀀스도 되돌려, 첫 작성 리뷰가 항상 id=1 이 되게 한다.
TRUNCATE TABLE place_reviews RESTART IDENTITY CASCADE;
TRUNCATE TABLE places RESTART IDENTITY CASCADE;

INSERT INTO places (id, name, category, location, address)
VALUES
    (601, '어니언 성수', 'CAFE', 'SRID=4326;POINT(127.0559 37.5446)'::geography, '서울 성동구 아차산로 100');
