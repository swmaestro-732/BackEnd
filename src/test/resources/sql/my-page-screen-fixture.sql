-- 마이페이지 코스 공개범위별 카운트와 created_at/id 키셋 페이지 통합 테스트 픽스처.
TRUNCATE TABLE users, courses RESTART IDENTITY CASCADE;

INSERT INTO users (nickname, handle, profile_image_url)
VALUES
    ('작성자', 'owner_handle', 'https://img/owner.jpg'),
    ('비팔로워 조회자', 'outsider_handle', 'https://img/outsider.jpg'),
    ('작성자의 팔로잉', 'reverse_follower_handle', 'https://img/reverse-follower.jpg'),
    ('진짜 팔로워', 'follower_handle', 'https://img/follower.jpg');

-- 팔로워 공개 카운트 노출/마스킹 검증용 관계:
--   (4→1) 조회자4가 작성자1을 팔로우 → 작성자 페이지에서 isFollowing=true → 팔로워 공개 카운트 노출
--   (1→3) 작성자1이 조회자3을 팔로우(역방향) → isFollower=true·isFollowing=false → 마스킹(0)
INSERT INTO follows (follower_id, following_id)
VALUES (4, 1), (1, 3);

-- PUBLIC 발행 코스 10개. n=1이 가장 최신이며 id도 1부터 순서대로 부여된다.
INSERT INTO courses (
    user_id, title, cover_image_url, category, is_published, visibility, likes_cnt, saves_cnt, created_at
)
SELECT
    1,
    '공개 코스 ' || n,
    'https://img/public-' || n || '.jpg',
    'CAFETOUR',
    true,
    'PUBLIC',
    n,
    n,
    TIMESTAMPTZ '2026-08-01T00:00:00Z' - (n * INTERVAL '1 day')
FROM generate_series(1, 10) AS series(n)
ORDER BY n;

-- FOLLOWER 코스는 PUBLIC 10번과 created_at이 같고 id가 더 커서 먼저 정렬된다.
-- 첫 페이지 경계의 동률 키를 커서 id로 정확히 이어가는지 함께 검증한다.
INSERT INTO courses (user_id, title, is_published, visibility, created_at)
VALUES
    (1, '팔로워 공개 코스', true, 'FOLLOWER', '2026-07-22T00:00:00Z'),
    (1, '비공개 코스', true, 'PRIVATE', '2026-07-19T00:00:00Z');

-- 카운트·목록 정의에서 제외돼야 하는 미발행·소프트 삭제·비활성 코스.
INSERT INTO courses (user_id, title, status, is_published, visibility, created_at, deleted_at)
VALUES
    (1, '공개 임시저장', 'ACTIVE', false, 'PUBLIC', '2026-07-18T00:00:00Z', NULL),
    (1, '삭제된 공개 코스', 'DELETED', true, 'PUBLIC', '2026-07-17T00:00:00Z', '2026-07-18T00:00:00Z'),
    (1, '숨김 팔로워 코스', 'HIDDEN', true, 'FOLLOWER', '2026-07-16T00:00:00Z', NULL);
