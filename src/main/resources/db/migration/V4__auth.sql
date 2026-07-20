ALTER TABLE users
    ADD COLUMN social_provider VARCHAR(20),
    ADD COLUMN social_id VARCHAR(255),
    ADD CONSTRAINT uq_users_social UNIQUE (social_provider, social_id);

ALTER TABLE users
    ADD CONSTRAINT users_social_pair_chk CHECK ((social_provider IS NULL) = (social_id IS NULL));

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
