-- 따라가기 세션이 따라갈 루트 (SCRUM-786). 시작 V2 가 받은 장소를 PLANNED 로 미리 넣고, 체크인하면 VISITED 로 바꾼다.
-- order_no 는 루트 순서다 — 루트 없이 시작한 세션(시작 V1·기존 세션)의 체크인 행은 NULL 이고 코스 장소를 그대로 따라간다.
-- 기존 행은 전부 체크인이라 VISITED. 기본값은 배포 중 옛 버전의 체크인 insert 가 깨지지 않게 남겨 둔다.
ALTER TABLE public.tracing_course_places
    ADD COLUMN status   VARCHAR(32) NOT NULL DEFAULT 'VISITED',  -- PLANNED / VISITED
    ADD COLUMN order_no SMALLINT,
    ALTER COLUMN visited_at DROP NOT NULL;                        -- PLANNED 이면 NULL
