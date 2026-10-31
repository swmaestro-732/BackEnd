-- 기기별 FCM 토큰. 로그인 시 등록, 로그아웃/탈퇴 시 삭제하는 계정 세션 데이터다(refresh_tokens 와 같은 성격).
-- 사용자마다 자기 기기 행을 가진다(user_id, device_id). 기기 ID 는 클라이언트 값이라 전역 유니크로 두면
-- 남의 기기 ID 를 먼저 등록해 막는 선점이 가능하다. 계정 전환 정리는 토큰(기기만 가진 값) 기준으로 한다.
CREATE TABLE device_tokens (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users (id),
    device_id varchar(128) NOT NULL,
    fcm_token varchar(1024) NOT NULL,
    platform varchar(16),
    app_build integer,
    created_at timestamptz NOT NULL DEFAULT now(),
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_device_tokens_user_device UNIQUE (user_id, device_id),
    -- 토큰 하나는 한 사용자에게만 연결된다. 같은 토큰의 동시 등록 경합은 뒤 요청이 유니크 위반으로 실패한다.
    CONSTRAINT uq_device_tokens_fcm_token UNIQUE (fcm_token)
);
