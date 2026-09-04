-- SCRUM-466 계정 분리: OAuth 자격증명(n) → Identity(1) → User(m, 멀티프로필)
-- expand 단계만: 새 테이블 신설 + 기존 users.social_* 를 1:1 로 이관하고 users 를 identity 에 연결한다.
-- users.social_* / uq_users_social / users_social_pair_chk 는 롤링 배포 중 구버전 인스턴스가 계속 쓰므로 이번엔 남겨둔다.
-- 드롭 + identity_id NOT NULL 승격은 배포 안정 후 후속 마이그레이션(V6).

CREATE TABLE public.identities (
    id         bigserial PRIMARY KEY,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TABLE public.oauth_credentials (
    id          bigserial PRIMARY KEY,
    identity_id bigint NOT NULL REFERENCES public.identities (id),
    provider    character varying(20) NOT NULL,
    social_id   character varying(255) NOT NULL,
    created_at  timestamp with time zone NOT NULL DEFAULT now(),
    updated_at  timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_oauth_credentials_provider_social UNIQUE (provider, social_id)
);

CREATE INDEX idx_oauth_credentials_identity ON public.oauth_credentials (identity_id);

ALTER TABLE public.users
    ADD COLUMN identity_id bigint REFERENCES public.identities (id),
    ADD COLUMN is_primary  boolean NOT NULL DEFAULT true;

CREATE INDEX idx_users_identity ON public.users (identity_id);

-- 백필: 기존 user 1명당 identity 1개를 만들어 연결하고, 소셜 쌍이 있으면 자격증명 1행을 옮긴다.
-- users_social_pair_chk 가 (provider, social_id) 동반 유무를, uq_users_social 이 유일성을 이미 보장하므로
-- 여기서 만들어지는 자격증명은 uq_oauth_credentials_provider_social 을 위반하지 않는다.
DO $$
DECLARE
    r                 record;
    new_identity_id   bigint;
BEGIN
    FOR r IN SELECT id, social_provider, social_id FROM public.users LOOP
        INSERT INTO public.identities DEFAULT VALUES RETURNING id INTO new_identity_id;
        UPDATE public.users SET identity_id = new_identity_id WHERE id = r.id;
        IF r.social_provider IS NOT NULL AND r.social_id IS NOT NULL THEN
            INSERT INTO public.oauth_credentials (identity_id, provider, social_id)
            VALUES (new_identity_id, r.social_provider, r.social_id);
        END IF;
    END LOOP;
END $$;
