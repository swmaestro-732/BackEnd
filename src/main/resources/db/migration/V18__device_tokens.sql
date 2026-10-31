-- 기기별 FCM 토큰. 로그인 시 등록, 로그아웃/탈퇴 시 삭제하는 계정 세션 데이터다(refresh_tokens 와 같은 성격).
-- 기기(device_id = Firebase Installation ID)당 한 행이라 토큰이 바뀌면 같은 행의 토큰만 교체된다.
CREATE TABLE device_tokens (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users (id),
    device_id varchar(128) NOT NULL,
    token varchar(4096) NOT NULL,
    platform varchar(16),
    app_build integer,
    created_at timestamptz NOT NULL DEFAULT now(),
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_device_tokens_device_id UNIQUE (device_id)
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);
