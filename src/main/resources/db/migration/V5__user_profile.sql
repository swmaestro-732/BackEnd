ALTER TABLE users
    ADD COLUMN handle VARCHAR(30);

ALTER TABLE users
    ADD CONSTRAINT uq_users_handle UNIQUE (handle);
