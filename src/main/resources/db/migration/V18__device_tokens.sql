-- 기기별 FCM 토큰. 사용자 참조는 도메인 경계를 넘어가므로 FK를 두지 않는다.
CREATE TABLE device_tokens (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL,
    token varchar(4096) NOT NULL,
    platform varchar(16),
    app_build integer,
    created_at timestamptz NOT NULL DEFAULT now(),
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_device_tokens_token UNIQUE (token)
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);
