-- 알림함(SCRUM-570): 사용자 활동을 수신자별 10분 묶음으로 저장한다.
-- 사용자와 코스는 교차 도메인 논리 참조이므로 FK를 두지 않는다.
-- 묶음 키의 유니크 제약으로 동시 기록도 한 행에 합산한다.
CREATE TABLE notifications (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL,
    type varchar(32) NOT NULL,
    target_type varchar(32) NOT NULL,
    target_id bigint NOT NULL,
    window_start timestamptz NOT NULL,
    last_actor_id bigint NOT NULL,
    -- 이 묶음의 서로 다른 행동자 수.
    actor_count integer NOT NULL DEFAULT 1,
    -- 기록 시점 스냅샷. 좋아요처럼 인원 수만 보이는 알림은 actor_nickname 이 NULL, 코스가 아닌 알림은 코스 칸이 NULL.
    -- course_image_url 은 원본 courses.cover_image_url 이 길이 제한 없는 text 라 같은 타입으로 둔다.
    actor_nickname varchar(20),
    course_title varchar(200),
    course_image_url text,
    read_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_notifications_bundle UNIQUE (user_id, type, target_type, target_id, window_start)
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id, id DESC);
CREATE INDEX idx_notifications_unread ON notifications (user_id) WHERE read_at IS NULL;
-- 탈퇴 정리(받는 사람 또는 마지막 행위자 기준 삭제)가 전체 스캔하지 않도록 행위자 쪽 인덱스를 둔다.
CREATE INDEX idx_notifications_last_actor_id ON notifications (last_actor_id);
-- 보관 기간 정리 잡이 오래된 알림을 지울 때 쓴다.
CREATE INDEX idx_notifications_created_at ON notifications (created_at);

-- 묶음마다 센 사람. 같은 사람이 다시 하면(좋아요 취소 후 재좋아요 등) 인원을 늘리지 않는다.
CREATE TABLE notification_actors (
    notification_id bigint NOT NULL REFERENCES notifications (id) ON DELETE CASCADE,
    actor_id bigint NOT NULL,
    PRIMARY KEY (notification_id, actor_id)
);

-- 탈퇴 정리(행동자 기준 삭제)가 전체 스캔하지 않도록 둔다.
CREATE INDEX idx_notification_actors_actor_id ON notification_actors (actor_id);

-- 설정 행이 없으면 모든 활동 알림을 허용하며 가입 시에는 행을 만들지 않는다.
CREATE TABLE notification_settings (
    user_id bigint PRIMARY KEY,
    activity_like boolean NOT NULL DEFAULT true,
    activity_comment boolean NOT NULL DEFAULT true,
    activity_follow boolean NOT NULL DEFAULT true,
    activity_review boolean NOT NULL DEFAULT true,
    updated_at timestamptz NOT NULL DEFAULT now()
);

-- 알림 화면이 들어가는 빌드 확정 시 UPSERT로 조정.
INSERT INTO app_version_policies (feature, platform, min_build) VALUES ('notification', 'android', 5);
