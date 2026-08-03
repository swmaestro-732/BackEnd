-- 로컬 개발용 시드 — 저장함 · 코스 탭(BFF) 실데이터.
-- dev 사용자(id=1, mock 소셜 로그인 subject)의 저장 코스 4건 + 코스별 장소(지도 핀) + 작성자/폴더/완주.
--
-- ⚠️ 통합 테스트(./gradlew test)는 로컬 chilsami DB 를 @Sql 픽스처로 초기화한다 —
--    테스트를 돌린 뒤에는 이 시드가 지워지므로 다시 실행해야 한다.
-- 실행:
--   docker exec -i backend-postgres-1 psql -U chilsami -d chilsami < scripts/dev-seed.sql
-- 확인: GET http://localhost:8080/service/v1/my/saved-courses  (mock 로그인 토큰 필요)
--
-- 테스트 픽스처(코스 1~3 등)와 충돌하지 않도록 코스는 201~204, 폴더는 301~304, 장소는 101~114 를 쓴다.
-- 전부 UPSERT/DELETE 후 INSERT 라 몇 번을 돌려도 같은 상태로 수렴한다.

BEGIN;

-- ── 1) 사용자: dev(1) + 작성자 2,3 ──────────────────────────────────────────
INSERT INTO users (id, status, handle, nickname, profile_image_url) VALUES
    (1, 0, 'hyunwoo',     '현우',       'https://images.unsplash.com/photo-1547425260-76bcadfb4f2c?w=200&q=80'),
    (2, 0, 'jiho_routes', '지호',       'https://images.unsplash.com/photo-1502685104226-ee32379fefbe?w=200&q=80'),
    (3, 0, 'slow_seoul',  '슬로우서울', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&q=80')
ON CONFLICT (id) DO UPDATE
    SET handle = EXCLUDED.handle, nickname = EXCLUDED.nickname,
        profile_image_url = EXCLUDED.profile_image_url, deleted_at = NULL;

SELECT setval('users_id_seq', (SELECT max(id) FROM users));

-- ── 2) 장소: 성수/연남/한남 (좌표는 지도 핀 표시용) ──────────────────────────
INSERT INTO places (id, status, name, category, location, address, image_url) VALUES
    (101, 'ACTIVE', '어니언 성수',       'CAFE',       ST_SetSRID(ST_MakePoint(127.0578, 37.5445), 4326)::geography, '서울 성동구 성수동2가 277-135', 'https://images.unsplash.com/photo-1517433670267-08bbd4be890f?w=600&q=80'),
    (102, 'ACTIVE', '대림창고 카페',     'CAFE',       ST_SetSRID(ST_MakePoint(127.0592, 37.5418), 4326)::geography, '서울 성동구 성수동2가 78-78',   'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=600&q=80'),
    (103, 'ACTIVE', '센터커피 성수',     'CAFE',       ST_SetSRID(ST_MakePoint(127.0537, 37.5463), 4326)::geography, '서울 성동구 성수동1가 656-566', 'https://images.unsplash.com/photo-1442512595331-e89e73853f31?w=600&q=80'),
    (104, 'ACTIVE', '자그마치',          'CULTURE',    ST_SetSRID(ST_MakePoint(127.0554, 37.5426), 4326)::geography, '서울 성동구 성수이로 88',       'https://images.unsplash.com/photo-1513151233558-d860c5398176?w=600&q=80'),
    (105, 'ACTIVE', '성수연방',          'SHOPPING',   ST_SetSRID(ST_MakePoint(127.0566, 37.5432), 4326)::geography, '서울 성동구 성수이로14길 14',   'https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=600&q=80'),
    (106, 'ACTIVE', '어반소스 성수',     'SHOPPING',   ST_SetSRID(ST_MakePoint(127.0551, 37.5448), 4326)::geography, '서울 성동구 연무장길 33',       'https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=600&q=80'),
    (107, 'ACTIVE', '연남 브런치하우스', 'RESTAURANT', ST_SetSRID(ST_MakePoint(126.9250, 37.5600), 4326)::geography, '서울 마포구 연남동 227-15',     'https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=600&q=80'),
    (108, 'ACTIVE', '연남동 골목카페',   'CAFE',       ST_SetSRID(ST_MakePoint(126.9236, 37.5619), 4326)::geography, '서울 마포구 성미산로 161',      'https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600&q=80'),
    (109, 'ACTIVE', '경의선숲길 연남',   'NATURE',     ST_SetSRID(ST_MakePoint(126.9262, 37.5588), 4326)::geography, '서울 마포구 연남동 385',        'https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=600&q=80'),
    (110, 'ACTIVE', '연남 소품샵',       'SHOPPING',   ST_SetSRID(ST_MakePoint(126.9270, 37.5628), 4326)::geography, '서울 마포구 동교로 245',        'https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=600&q=80'),
    (111, 'ACTIVE', '연남 화덕피자',     'RESTAURANT', ST_SetSRID(ST_MakePoint(126.9243, 37.5645), 4326)::geography, '서울 마포구 월드컵북로6길 61',  'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600&q=80'),
    (112, 'ACTIVE', '리움미술관',        'CULTURE',    ST_SetSRID(ST_MakePoint(127.0000, 37.5385), 4326)::geography, '서울 용산구 이태원로55길 60-16','https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=600&q=80'),
    (113, 'ACTIVE', '한남 갤러리길',     'CULTURE',    ST_SetSRID(ST_MakePoint(127.0043, 37.5346), 4326)::geography, '서울 용산구 대사관로 35',       'https://images.unsplash.com/photo-1513151233558-d860c5398176?w=600&q=80'),
    (114, 'ACTIVE', '블루스퀘어',        'LANDMARK',   ST_SetSRID(ST_MakePoint(127.0086, 37.5326), 4326)::geography, '서울 용산구 이태원로 294',      'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=600&q=80')
ON CONFLICT (id) DO UPDATE
    SET name = EXCLUDED.name, category = EXCLUDED.category, location = EXCLUDED.location,
        address = EXCLUDED.address, image_url = EXCLUDED.image_url, deleted_at = NULL, status = 'ACTIVE';

SELECT setval('places_id_seq', (SELECT max(id) FROM places));

-- ── 3) 코스(201~204) — 지역(area)·테마(category)·커버·작성자 ──────────────────
INSERT INTO courses (id, user_id, title, description, area, category, is_published, visibility, cover_image_url) VALUES
    (201, 2, '비 오는 날 성수 감성 카페 코스', '비 오는 날 걷기 좋은 성수 카페 골목 코스', '성수', 'CAFETOUR', true, 'PUBLIC', 'https://images.unsplash.com/photo-1445116572660-236099ec97a0?w=600&q=80&auto=format&fit=crop'),
    (202, 3, '성수 골목 소품샵 산책',          '성수 골목 소품샵을 천천히 도는 산책 코스', '성수', 'SHOPPING', true, 'PUBLIC', 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=600&q=80&auto=format&fit=crop'),
    (203, 3, '주말 연남 느긋한 브런치 산책',   '주말 연남에서 즐기는 느긋한 브런치 코스', '연남', 'FOOD',     true, 'PUBLIC', 'https://images.unsplash.com/photo-1528605248644-14dd04022da1?w=600&q=80&auto=format&fit=crop'),
    (204, 1, '한남동 갤러리 하나씩 도장깨기',   '한남동 갤러리를 하나씩 도는 전시 코스',   '한남', 'CULTURE',  true, 'PUBLIC', 'https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600&q=80&auto=format&fit=crop')
ON CONFLICT (id) DO UPDATE
    SET user_id = EXCLUDED.user_id, title = EXCLUDED.title, description = EXCLUDED.description,
        area = EXCLUDED.area, category = EXCLUDED.category, is_published = EXCLUDED.is_published,
        visibility = EXCLUDED.visibility, cover_image_url = EXCLUDED.cover_image_url,
        deleted_at = NULL, status = 'ACTIVE';

SELECT setval('courses_id_seq', (SELECT max(id) FROM courses));

-- ── 4) 코스-장소 연결(order_no·도보시간) ─────────────────────────────────────
DELETE FROM course_place_images WHERE course_place_id IN (SELECT id FROM course_places WHERE course_id IN (201,202,203,204));
DELETE FROM course_places WHERE course_id IN (201,202,203,204);

INSERT INTO course_places (course_id, place_id, order_no, caption, walking_minutes) VALUES
    (201, 101, 0, '통창 너머 비 내리는 골목', 8),
    (201, 103, 1, '핸드드립 한 잔',           6),
    (201, 104, 2, '전시 겸 카페',             7),
    (201, 102, 3, '창고 개조 대형 카페',      NULL),
    (202, 105, 0, '복합문화공간 성수연방',    9),
    (202, 106, 1, '감각적인 리빙 소품',       6),
    (202, 102, 2, '마무리 커피 한 잔',        NULL),
    (203, 107, 0, '느긋한 주말 브런치',       7),
    (203, 108, 1, '골목 안 조용한 카페',      5),
    (203, 109, 2, '숲길 산책',                8),
    (203, 110, 3, '아기자기 소품 구경',       6),
    (203, 111, 4, '화덕피자로 마무리',        NULL),
    (204, 112, 0, '리움에서 시작',            10),
    (204, 113, 1, '골목 갤러리 순례',         7),
    (204, 114, 2, '공연장 앞에서 마무리',     NULL);

-- 코스에서 이 장소에 올린 사진(course_place_images) — 장소 자체 이미지와 다른, 작성자가 올린 코스 사진.
-- 한 장소에 여러 장 가능하므로 order_no 0·1 두 장씩 넣는다.
INSERT INTO course_place_images (course_place_id, image_url, order_no)
SELECT cp.id, 'https://images.unsplash.com/' || img.photo_id || '?w=600&q=80&auto=format&fit=crop', img.order_no
FROM course_places cp
CROSS JOIN LATERAL (
    VALUES
        (0, (ARRAY['photo-1504674900247-0877df9cc836','photo-1414235077428-338989a2e8c0','photo-1517248135467-4c7edcad34c4','photo-1533777324565-a040eb52facd','photo-1559925393-8be0ec4767c8'])[(cp.order_no % 5) + 1]),
        (1, (ARRAY['photo-1466978913421-dad2ebd01d17','photo-1481833761820-0509d3217039','photo-1498804103079-a6351b050096','photo-1521017432531-fbd92d768814','photo-1519690889869-e705e59f72e1'])[(cp.order_no % 5) + 1])
) AS img(order_no, photo_id)
WHERE cp.course_id IN (201,202,203,204);

-- ── 5) 저장 폴더/저장/완주 (user 1) — 재실행 위해 먼저 비운다 ─────────────────
DELETE FROM tracing_courses WHERE user_id = 1;
DELETE FROM saved_courses WHERE user_id = 1;
DELETE FROM saved_course_folders WHERE user_id = 1;

INSERT INTO saved_course_folders (id, user_id, name, order_no) VALUES
    (301, 1, '데이트 코스',    0),
    (302, 1, '주말 나들이',    1),
    (303, 1, '혼자 걷기',      2),
    (304, 1, '가보고 싶은 곳', 3);
SELECT setval('saved_course_folders_id_seq', (SELECT max(id) FROM saved_course_folders));

-- 저장 시각 오래된 것부터 넣어 최신 저장이 목록 상단(id desc)에 오게 한다.
INSERT INTO saved_courses (user_id, course_id, folder_id, created_at) VALUES
    (1, 204, 303, '2026-03-10T02:00:00Z'),
    (1, 203, 302, '2026-07-13T08:30:00Z'),
    (1, 201, 301, '2026-07-15T13:05:00Z'),
    (1, 202, 301, '2026-07-18T10:40:00Z');

-- 완주(따라가기): user 1 이 한남 갤러리 코스(204)를 완주
INSERT INTO tracing_courses (user_id, course_id, created_at) VALUES (1, 204, '2026-03-12T07:30:00Z');

COMMIT;
