-- 기기별 FCM 토큰. 로그인 시 등록, 로그아웃/탈퇴 시 삭제하는 계정 세션 데이터다(refresh_tokens 와 같은 성격).
CREATE TABLE device_tokens (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users (id),
    token varchar(4096) NOT NULL,
    platform varchar(16),
    app_build integer,
    created_at timestamptz NOT NULL DEFAULT now(),
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_device_tokens_token UNIQUE (token)
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);
