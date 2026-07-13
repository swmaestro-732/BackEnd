TRUNCATE TABLE users, places, place_business_hours, place_reviews, place_review_photos RESTART IDENTITY CASCADE;

INSERT INTO users (nickname, profile_image_url)
VALUES ('현우님', 'https://cdn.example.com/users/1.jpg');

INSERT INTO places (name, category, location, address, image_url, business_status)
VALUES ('어니언 성수', '카페', ST_GeogFromText('POINT(127.0559 37.5446)'),
        '서울 성동구 아차산로 100', 'https://cdn.example.com/places/1/1.jpg', 0);

-- 전 요일 동일 영업시간 → "매일 11:00 – 21:00"
INSERT INTO place_business_hours (place_id, day_of_week, open_time, close_time)
SELECT 1, d, TIME '11:00', TIME '21:00'
FROM generate_series(0, 6) AS d;

-- 평균 (5+4+5)/3 = 4.666… → 4.7 반올림, 미리보기는 최신 2개
-- HIDDEN(status=1) 리뷰는 최신이어도 미리보기·개수·평점에서 제외되어야 한다
INSERT INTO place_reviews (place_id, user_id, rating, content, created_at, status)
VALUES (1, 1, 5, '팡도르가 정말 맛있어요.', now() - interval '3 days', 0),
       (1, 1, 4, '빵이 다양해요.', now() - interval '5 days', 0),
       (1, 1, 5, '조용해요.', now() - interval '10 days', 0),
       (1, 1, 1, '숨김 처리된 리뷰.', now() - interval '1 day', 1);

INSERT INTO place_review_photos (place_review_id, image_url, order_no)
VALUES (1, 'https://cdn.example.com/reviews/1/1.jpg', 0);
