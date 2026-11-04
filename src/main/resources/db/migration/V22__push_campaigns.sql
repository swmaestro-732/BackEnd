-- 어드민 마케팅 푸시 발송 기록(SCRUM-779). 발송 한 번에 1행이며 실제로 나간 제목과 본문을 남긴다.
-- admin_email 은 발송한 어드민이다. 어드민이 admins 에서 빠져도 기록은 남기므로 FK 를 두지 않는다.
CREATE TABLE push_campaigns (
    id bigserial PRIMARY KEY,
    admin_email text NOT NULL,
    title text NOT NULL,
    body text NOT NULL,
    link text NOT NULL,
    -- 야간(KST 21시~8시) 발송이면 야간 수신 동의 기기에만 보냈다.
    night boolean NOT NULL,
    recipient_count int NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);
