-- 내 계획 목록(커서 페이지) 통합 테스트 픽스처 — 기본 시드에 계획 3건을 미리 심는다.
-- 정렬 키(updated_at DESC, id DESC)를 예측 가능하게 두려고 updated_at 을 명시한다.
-- 기대 순서: 12(최신) → 11 → 10. 13 은 소프트 삭제라 목록에서 빠지고, 14 는 타인 소유라 보이지 않는다.
TRUNCATE TABLE plan_places, plans, courses, places, users RESTART IDENTITY CASCADE;

INSERT INTO users (nickname) VALUES ('계획소유자'), ('타인');

INSERT INTO places (name, category, location, address)
VALUES
    ('카페A', 'CAFE', 'SRID=4326;POINT(127.05 37.54)'::geography, '서울 성수동 1'),
    ('카페B', 'CAFE', 'SRID=4326;POINT(127.06 37.55)'::geography, '서울 성수동 2');

INSERT INTO plans (id, user_id, title, memo, planned_date, created_at, updated_at, deleted_at)
VALUES
    (10, 1, '가장 오래된 계획', NULL, NULL, '2026-09-01T00:00:00Z', '2026-09-01T00:00:00Z', NULL),
    (11, 1, '중간 계획', '메모', '2026-09-20', '2026-09-02T00:00:00Z', '2026-09-02T00:00:00Z', NULL),
    (12, 1, '최근 계획', NULL, NULL, '2026-09-03T00:00:00Z', '2026-09-03T00:00:00Z', NULL),
    (13, 1, '삭제된 계획', NULL, NULL, '2026-09-04T00:00:00Z', '2026-09-04T00:00:00Z', '2026-09-05T00:00:00Z'),
    (14, 2, '타인 계획', NULL, NULL, '2026-09-04T00:00:00Z', '2026-09-04T00:00:00Z', NULL);

-- 장소 수(placeCount) 검증: 10 은 2곳, 11 은 1곳, 12 는 0곳(장소 없는 행도 목록에 나와야 한다).
INSERT INTO plan_places (plan_id, place_id, order_no, memo, walking_minutes)
VALUES
    (10, 1, 0, '첫 장소', 7),
    (10, 2, 1, NULL, NULL),
    (11, 1, 0, NULL, NULL);
