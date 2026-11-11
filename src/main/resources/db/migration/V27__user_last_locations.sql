-- 홈 진입 시 받은 로그인 사용자의 최근 좌표 1건(SCRUM-780). 추천 코스 위치 반영(SCRUM-781)에 쓴다.
-- 이력은 남기지 않고 덮어쓴다. 탈퇴 시 행을 지운다.
CREATE TABLE user_last_locations (
    user_id bigint PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    location geography(Point, 4326) NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);
