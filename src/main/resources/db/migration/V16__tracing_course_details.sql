-- 따라가기 종료 기록 상세 (SCRUM-583). 종료 요청으로 받은 소요 시간·이동 거리(클라이언트 측정값)와 방문한 장소를 남긴다.
-- 기존 added_places 는 의미(따라가기 중 추가한 장소)가 달라 쓰지 않고 방문 기록 테이블을 따로 둔다.
ALTER TABLE public.tracing_courses
    ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 0,  -- 소요 시간(분)
    ADD COLUMN distance_meters  INTEGER NOT NULL DEFAULT 0;  -- 이동 거리(m)

-- 따라가기 중 방문(체크)한 장소. 요청 순서를 order_no 로, 도착 시각을 visited_at 으로 둔다.
CREATE TABLE public.tracing_course_places (
    id                BIGSERIAL   PRIMARY KEY,
    tracing_course_id BIGINT      NOT NULL,
    place_id          BIGINT      NOT NULL,                  -- 교차 도메인(place): FK 없음
    order_no          SMALLINT    NOT NULL DEFAULT 0,
    visited_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT tracing_course_places_tracing_course_id_fkey
        FOREIGN KEY (tracing_course_id) REFERENCES public.tracing_courses(id),
    CONSTRAINT tracing_course_places_tracing_course_id_order_no_key UNIQUE (tracing_course_id, order_no)
);

-- 기능별 최소 빌드 정책 — 따라가기(course-tracing) 키 신설. 따라가기 API 가 들어가는 빌드가 5 라 그 미만은 426.
-- 값 변경은 UPSERT, 롤백은 행 삭제(V9 plan 방식).
INSERT INTO app_version_policies (feature, platform, min_build) VALUES
    ('course-tracing', 'android', 5);
