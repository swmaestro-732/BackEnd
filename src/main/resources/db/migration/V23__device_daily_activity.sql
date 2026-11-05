-- 기기별 일별 활동(SCRUM-783). 코호트 리텐션(D1/D7/D30)을 내려면 덮어쓰는 last_seen_at 이 아니라 활동한 날짜가 전부 있어야 한다.
-- X-Device-Id 를 보낸 요청이 있으면 그날(KST) 행이 하나 생긴다. 재설치하면 새 기기로 잡히므로 사람 단위는 user_id 로 본다.
-- user_id 는 그날 한 번이라도 로그인 상태로 요청했으면 채워진다(처음 로그인한 계정 유지).
CREATE TABLE device_daily_activity (
    device_id varchar(128) NOT NULL,
    active_date date NOT NULL,
    user_id bigint,
    PRIMARY KEY (device_id, active_date)
);

-- 코호트별 집계가 날짜로 자른다.
CREATE INDEX idx_device_daily_activity_date ON device_daily_activity (active_date);
