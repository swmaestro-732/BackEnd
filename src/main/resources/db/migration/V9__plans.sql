-- 코스 계획 (SCRUM-535). 코스를 가기 전 사용자가 장소·순서·메모만 담아 두는 사적 데이터 —
-- 계획(과거) → 따라가기(현재) → 코스 생성(미래) 흐름의 첫 단계다. 공개범위·태그·사진·카운터는 없다.
-- 도보 시간·지도 경로는 저장하지 않는다(필요하면 조회 시 direction 으로 계산).
-- 인덱스는 여기서 걸지 않는다 — 조회 인덱스는 나중에 한 번에 정리한다.
CREATE TABLE public.plans (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL,         -- 교차 도메인(user): courses 와 같이 FK 없음
    title            VARCHAR(200) NOT NULL DEFAULT '',
    memo             TEXT,                          -- 계획 전체 메모
    planned_date     DATE,                          -- 가려는 날(선택)
    source_course_id BIGINT,                        -- 계획 복제 원본 코스(같은 도메인). 자유 계획이면 NULL
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at       TIMESTAMPTZ,                   -- soft delete. 모든 조회가 deleted_at IS NULL 로 거른다
    CONSTRAINT plans_source_course_id_fkey FOREIGN KEY (source_course_id) REFERENCES public.courses(id)
);

-- 계획에 담긴 장소. course_places 와 같은 모양이되 사진·도보 시간 없이 순서·메모만 둔다.
-- 편집은 전체 치환(삭제 후 재삽입)이라 (plan_id, order_no) UNIQUE 로 순서 중복을 DB 에서도 막는다.
CREATE TABLE public.plan_places (
    id       BIGSERIAL PRIMARY KEY,
    plan_id  BIGINT    NOT NULL,
    place_id BIGINT    NOT NULL,                    -- 교차 도메인(place): FK 없음
    order_no SMALLINT  NOT NULL DEFAULT 0,
    memo     VARCHAR(500),                          -- 장소별 메모
    CONSTRAINT plan_places_plan_id_fkey FOREIGN KEY (plan_id) REFERENCES public.plans(id),
    CONSTRAINT plan_places_plan_id_order_no_key UNIQUE (plan_id, order_no)
);

-- 계획 기능의 앱 최소 빌드 정책(강제 업데이트, V7 과 같은 규칙). AppFeature.PLAN 키 = 'plan'.
-- 계획 API 가 들어가는 빌드가 5 라 그 미만은 426. 값 변경은 UPSERT, 롤백은 행 삭제.
INSERT INTO app_version_policies (feature, platform, min_build) VALUES
    ('plan', 'android', 5);
