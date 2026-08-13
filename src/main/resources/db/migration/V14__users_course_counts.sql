-- 코스 개수 비정규화: 매 마이페이지 조회마다 courses GROUP BY COUNT 하던 것을
-- users 에 공개범위별 저장 카운터로 옮긴다(followers_cnt/saves_cnt 와 동일한 캐시 컬럼 패턴).
-- 조회자별 마스킹(본인=전체, 팔로워=공개+팔로워, 그외=공개)을 위해 버킷을 나눠 저장하고,
-- 코스 발행/공개범위변경/삭제 시 CourseService 가 ±1 로 유지한다.
-- 아무도 증감하지 않던 단일 총합 컬럼 courses_cnt 는 이 세 버킷으로 대체되므로 함께 제거한다
-- (응답 coursesCnt 는 조회자 상황에 맞게 세 버킷을 합산해 계산).
ALTER TABLE users
    ADD COLUMN public_courses_cnt   INT NOT NULL DEFAULT 0,
    ADD COLUMN follower_courses_cnt INT NOT NULL DEFAULT 0,
    ADD COLUMN private_courses_cnt  INT NOT NULL DEFAULT 0;

-- 기존 발행·활성·미삭제 코스로 백필(카운트 술어는 CourseRepository 와 동일).
UPDATE users u
SET public_courses_cnt = counts.public_cnt,
    follower_courses_cnt = counts.follower_cnt,
    private_courses_cnt  = counts.private_cnt
FROM (
    SELECT c.user_id,
           count(*) FILTER (WHERE c.visibility = 'PUBLIC')   AS public_cnt,
           count(*) FILTER (WHERE c.visibility = 'FOLLOWER') AS follower_cnt,
           count(*) FILTER (WHERE c.visibility = 'PRIVATE')  AS private_cnt
    FROM courses c
    WHERE c.is_published
      AND c.status = 'ACTIVE'
      AND c.deleted_at IS NULL
    GROUP BY c.user_id
) counts
WHERE u.id = counts.user_id;

ALTER TABLE users DROP COLUMN courses_cnt;
