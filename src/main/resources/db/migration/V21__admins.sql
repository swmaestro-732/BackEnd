-- 어드민 계정(SCRUM-768). 앱 사용자(users)와 별개이며 구글 계정 이메일로 식별한다.
-- 지정은 DB 에 직접 INSERT 한다. 이메일은 소문자로 저장한다(로그인 시 소문자로 비교).
CREATE TABLE admins (
    email varchar(320) PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT now()
);
