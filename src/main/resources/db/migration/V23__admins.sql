-- 어드민 계정(SCRUM-768). 앱 사용자(users)와 별개이며 구글 계정(이메일과 sub)으로 식별한다.
-- 등록 절차: 등록할 사람이 어드민 로그인을 시도하면 403 응답(과 서버 로그)에 본인 이메일과 googleSub 가 나온다.
-- 운영자가 본인에게 직접 확인한 뒤 두 값을 함께 INSERT 한다. 이메일은 소문자로 저장한다(로그인 시 소문자로 비교).
-- 같은 이메일이라도 등록한 구글 계정(sub)만 통과하므로, 이메일 주인이 바뀌어도 이전 주인은 로그인할 수 없다.
CREATE TABLE admins (
    email varchar(320) PRIMARY KEY,
    google_sub varchar(255) NOT NULL UNIQUE,
    created_at timestamptz NOT NULL DEFAULT now()
);
