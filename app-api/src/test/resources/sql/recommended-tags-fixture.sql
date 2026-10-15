TRUNCATE TABLE users, courses, course_places, course_tags, tags RESTART IDENTITY CASCADE;

INSERT INTO users (nickname) VALUES ('코스작성자');

INSERT INTO courses (user_id, title) VALUES (1, '코스1'), (1, '코스2'), (1, '코스3');

INSERT INTO tags (name) VALUES ('감성카페'), ('통창뷰'), ('데이트');

-- 장소 100은 코스1·2에 담겨 있다
INSERT INTO course_places (course_id, place_id, order_no) VALUES (1, 100, 1), (2, 100, 1);

-- 감성카페: 코스1·2 (장소 기반 1위) / 통창뷰: 코스1 / 데이트: 코스3 (장소 미포함 → 인기 태그 채움 대상)
INSERT INTO course_tags (course_id, tag_id) VALUES (1, 1), (2, 1), (1, 2), (3, 3);
