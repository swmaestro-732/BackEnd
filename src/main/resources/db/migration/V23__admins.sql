-- 어드민 계정(SCRUM-768). 앱 사용자(users)와 별개이며 구글 계정 이메일로 지정한다.
-- 지정은 DB 에 이메일만 직접 INSERT 한다. 이메일은 소문자로 저장한다(로그인 시 소문자로 비교).
CREATE TABLE admins (
    email varchar(320) PRIMARY KEY,
    -- 구글 계정 id(sub). 첫 로그인 때 채우고 이후엔 같은 계정만 통과한다. 다시 묶으려면 NULL 로 되돌린다.
    google_sub varchar(255),
    created_at timestamptz NOT NULL DEFAULT now()
);

-- 한 구글 계정은 한 어드민 행에만 묶인다. 아직 묶이지 않은 행(NULL)은 여럿이어도 된다.
CREATE UNIQUE INDEX uq_admins_google_sub ON admins (google_sub) WHERE google_sub IS NOT NULL;
