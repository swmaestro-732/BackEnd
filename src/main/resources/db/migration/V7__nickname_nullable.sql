-- 소셜 로그인/회원가입 통합: 최소 계정(nickname·handle 둘 다 null)을 먼저 만들고
-- 온보딩(PATCH /my/profile)에서 nickname·handle 을 채운다. 이를 위해 nickname 을 nullable 로 완화.
-- UNIQUE 제약은 유지한다(Postgres 는 NULL 다중 허용이므로 최소 계정 다건 공존 가능).
ALTER TABLE users ALTER COLUMN nickname DROP NOT NULL;
