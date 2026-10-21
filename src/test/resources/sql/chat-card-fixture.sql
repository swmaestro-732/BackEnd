-- 채팅 카드 공유용 장소·코스. chat-fixture.sql 다음에 실행한다(작성자 = 사용자 1).
TRUNCATE TABLE places, courses RESTART IDENTITY CASCADE;

INSERT INTO places (id, name, category, location, address, image_url)
VALUES (501, '어니언 성수', 'CAFE', 'SRID=4326;POINT(127.0559 37.5446)'::geography, '서울 성동구 아차산로 100',
        'https://cdn.example.com/places/501/1.jpg');

-- course 1: 공유 가능(전체 공개·발행). course 2: 비공개. course 3: 전체 공개지만 임시저장.
INSERT INTO courses (user_id, title, cover_image_url, category, area, is_published, visibility)
VALUES (1, '성수 카페 투어', 'https://img/c1.jpg', 'CAFETOUR', '성수', true, 'PUBLIC'),
       (1, '비공개 코스', 'https://img/c2.jpg', 'CAFETOUR', NULL, true, 'PRIVATE'),
       (1, '임시저장 코스', '', NULL, NULL, false, 'PUBLIC');

INSERT INTO course_places (course_id, place_id, order_no, walking_minutes)
VALUES (1, 501, 0, 5), (1, 502, 1, NULL);
