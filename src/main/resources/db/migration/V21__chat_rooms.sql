-- 지역 채팅방·멤버 (SCRUM-546). 메시지는 MongoDB 에 둔다.
-- 방과 지역을 분리해, 구를 쪼개거나(H3_CELL) 묶을(AREA_PREFIX 여러 행) 때 매핑 행만 바꾼다.
-- region_type·type 은 enum 이름(대문자)이 DB 저장 계약이다.
CREATE TABLE public.chat_rooms (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

-- 한 지역은 한 방에만 속한다.
CREATE TABLE public.chat_room_regions (
    room_id      BIGINT      NOT NULL REFERENCES public.chat_rooms (id),
    region_type  VARCHAR(32) NOT NULL,
    region_value VARCHAR(20) NOT NULL,
    PRIMARY KEY (region_type, region_value)
);
CREATE INDEX idx_chat_room_regions_room ON public.chat_room_regions (room_id);

-- last_read_seq 는 MongoDB 방 카운터의 seq 다. 안 읽은 수 = 카운터 lastSeq - last_read_seq.
-- user_id 는 교차 도메인이라 FK 를 두지 않는다.
CREATE TABLE public.chat_members (
    room_id       BIGINT      NOT NULL REFERENCES public.chat_rooms (id),
    user_id       BIGINT      NOT NULL,
    type          VARCHAR(32) NOT NULL,
    last_read_seq BIGINT      DEFAULT 0 NOT NULL,
    joined_at     TIMESTAMPTZ DEFAULT now() NOT NULL,
    PRIMARY KEY (room_id, user_id)
);
CREATE INDEX idx_chat_members_user ON public.chat_members (user_id);

-- 시군구마다 방 하나. 세종은 시군구 이름이 없어 시도 이름만 쓴다.
DO $$
DECLARE
    s           RECORD;
    new_room_id BIGINT;
BEGIN
    FOR s IN
        SELECT b.sigungu_code,
               min(a.sido_name) || coalesce(' ' || min(a.sigungu_name), '') AS name
        FROM public.sigungu_boundaries b
        JOIN public.area a ON a.sigungu_code = b.sigungu_code
        GROUP BY b.sigungu_code
        ORDER BY b.sigungu_code
    LOOP
        INSERT INTO public.chat_rooms (name) VALUES (s.name) RETURNING id INTO new_room_id;
        INSERT INTO public.chat_room_regions (room_id, region_type, region_value)
        VALUES (new_room_id, 'AREA_PREFIX', s.sigungu_code);
    END LOOP;
END $$;
