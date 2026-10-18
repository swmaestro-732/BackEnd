-- 작성자 코스·팔로워 목록의 조회 인덱스 (SCRUM-516).
-- 일반 CREATE INDEX는 생성 중 쓰기를 막으므로 배포 시 실행 시간과 잠금을 확인한다.

-- findPublishedByAuthor: 작성자와 발행·활성·미삭제 조건으로 범위를 좁히고
-- created_at DESC, id DESC 키셋 정렬을 지원한다. 공개 범위(visibility)는 조회에서 필터링한다.
CREATE INDEX idx_courses_author_published
    ON public.courses (user_id, created_at DESC, id DESC)
    WHERE is_published = true AND status = 'ACTIVE' AND deleted_at IS NULL;

-- findFollowers: following_id로 범위를 좁히고 id DESC 키셋 정렬을 지원한다.
-- 기존 UNIQUE(follower_id, following_id)는 follower_id가 선두인 반대 방향 조회용이다.
CREATE INDEX idx_follows_following
    ON public.follows (following_id, id DESC);
