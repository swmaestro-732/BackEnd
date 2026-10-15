-- 홈 추천 코스(공개 코스 피드)의 조회 인덱스 (SCRUM-532).
-- 일반 CREATE INDEX는 생성 중 쓰기를 막으므로 배포 시 실행 시간과 잠금을 확인한다.

-- findPublishedPublic: 공개·발행·활성·미삭제 코스만 담고 saves_cnt DESC, created_at DESC, id DESC 키셋 정렬을 지원한다.
-- 커서는 행 비교 (saves_cnt, created_at, id) < (...) 로 보내야 인덱스 탐색 조건이 된다.
-- 조건은 CourseRepository.findPublishedPublic 과 같아야 플래너가 채택한다.
CREATE INDEX idx_courses_public_feed
    ON public.courses (saves_cnt DESC, created_at DESC, id DESC)
    WHERE is_published = true AND status = 'ACTIVE' AND deleted_at IS NULL AND visibility = 'PUBLIC';
