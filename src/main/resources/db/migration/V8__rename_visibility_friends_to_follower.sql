-- CourseVisibility 상수 FRIENDS → FOLLOWER 로 개명(팔로우 기반 공개로 의미 명확화).
-- visibility 는 enum 이름을 그대로 저장(V3 에서 VARCHAR 전환)하므로 기존 행의 저장값도 갱신한다.
-- DEFAULT 는 'PUBLIC' 이라 영향 없음.
UPDATE courses
SET visibility = 'FOLLOWER'
WHERE visibility = 'FRIENDS';
