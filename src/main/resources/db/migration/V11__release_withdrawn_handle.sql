-- 탈퇴 시 handle 을 즉시 해제하는 정책 전환에 맞춰, 이미 탈퇴(soft delete)된 행의 handle 도 NULL 로 비운다.
-- handle 은 nullable 이고 uq_users_handle UNIQUE 는 NULL 다중 허용이라 제약 변경은 불필요하다.
-- (신규 탈퇴는 UserRepository.softDelete 가 handle 을 NULL 로 밀어 처리한다.)
UPDATE users SET handle = NULL WHERE deleted_at IS NOT NULL;
