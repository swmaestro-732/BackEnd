-- username(로그인 ID)이 곧 @handle 이었으므로 handle 로 통일한다.
-- 새 컬럼을 만들지 않고 기존 username 을 handle 로 리네임(+ 길이 확장 + UNIQUE).
ALTER TABLE users
    RENAME COLUMN username TO handle;

ALTER TABLE users
    ALTER COLUMN handle TYPE VARCHAR(30);

ALTER TABLE users
    ADD CONSTRAINT uq_users_handle UNIQUE (handle);
