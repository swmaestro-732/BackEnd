-- 기기별 일별 활동(SCRUM-783). 코호트 리텐션(D1/D7/D30)을 내려면 덮어쓰는 last_seen_at 이 아니라 활동한 날짜가 전부 있어야 한다.
-- X-Device-Id 를 보낸 요청이 있으면 그날(KST) 행이 하나 생긴다. 재설치하면 새 기기로 잡히므로 사람 단위는 user_id 로 본다.
-- user_id 는 그날 한 번이라도 로그인 상태로 요청했으면 채워진다(처음 로그인한 계정 유지). 탈퇴하면 NULL 로 지운다.
CREATE TABLE device_daily_activity (
    device_id varchar(128) NOT NULL,
    active_date date NOT NULL,
    user_id bigint,
    -- X-App-Platform 헤더(android, ios). 헤더가 없거나 알 수 없는 값이면 NULL.
    platform varchar(10),
    PRIMARY KEY (device_id, active_date)
);

-- 코호트별 집계가 날짜로 자른다.
CREATE INDEX idx_device_daily_activity_date ON device_daily_activity (active_date);

-- 탈퇴 시 user_id 를 지울 때 쓴다.
CREATE INDEX idx_device_daily_activity_user_id ON device_daily_activity (user_id) WHERE user_id IS NOT NULL;
