-- 기기 단위 푸시 등록(SCRUM-756). 비로그인 기기도 FCM 토큰과 광고성 수신 동의를 가진다.
-- 로그인 계정은 user_id 로만 연결하고(한 기기에 한 계정), 로그아웃과 탈퇴는 연결만 끊어 토큰과 동의는 기기에 남긴다.
-- device_id 는 앱 설치마다 만든 UUID(X-Device-Id)다.
CREATE TABLE devices (
    device_id varchar(128) PRIMARY KEY,
    user_id bigint REFERENCES users (id),
    -- 토큰 하나는 한 기기에만 있다. 동의를 먼저 보내 토큰이 아직 없는 기기는 NULL.
    fcm_token varchar(1024) UNIQUE,
    platform varchar(16),
    app_build integer,
    -- 광고성 정보 수신 동의 시각(정보통신망법 제50조). NULL 이면 미동의. 야간(21시~8시)은 별도 동의.
    marketing_agreed_at timestamptz,
    night_agreed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    last_seen_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_devices_user_id ON devices (user_id) WHERE user_id IS NOT NULL;

-- device_tokens(V18) 이관. 같은 기기 ID 를 여러 계정이 가진 경우 최근 행 하나만 옮긴다(토큰은 이미 유일).
INSERT INTO devices (device_id, user_id, fcm_token, platform, app_build, created_at, last_seen_at)
SELECT DISTINCT ON (device_id) device_id, user_id, fcm_token, platform, app_build, created_at, last_seen_at
FROM device_tokens
ORDER BY device_id, last_seen_at DESC;

DROP TABLE device_tokens;
