-- 어드민 조치 감사 기록(SCRUM-752). 코스 수정, 숨김, 숨김 해제, 삭제, 복원(일괄 조치는 건별)마다 1행을 남긴다.
-- 장소, 신고 관리도 같은 테이블을 쓴다(target_type 으로 구분). 어드민이 admins 에서 빠져도 기록은 남기므로 FK 를 두지 않는다.
CREATE TABLE admin_actions (
    id bigserial PRIMARY KEY,
    admin_email varchar(320) NOT NULL,
    target_type varchar(32) NOT NULL,
    target_id bigint NOT NULL,
    action varchar(32) NOT NULL,
    memo varchar(500),
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 대상별 처리 이력(신고 화면 등)을 최근순으로 읽는다.
CREATE INDEX idx_admin_actions_target ON admin_actions (target_type, target_id, created_at DESC);
