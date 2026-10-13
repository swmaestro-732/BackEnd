-- 따라가기 세션 (SCRUM-583). 시작 시 IN_PROGRESS 행을 만들고, 장소 체크인은 그때그때 tracing_course_places 에 쌓이며,
-- 종료 시 COMPLETED 로 바꾸면서 소요 시간·이동 거리(클라이언트 측정값)를 기록한다. 앱이 중간에 꺼져도 서버에 진행 상태가 남는다.
-- 기존 added_places 는 의미(따라가기 중 추가한 장소)가 달라 쓰지 않고 방문 기록 테이블을 따로 둔다.
-- 기존 행은 옛 의미(행 존재 = 완주)대로 COMPLETED 로 본다. created_at 이 시작 시각이다.
ALTER TABLE public.tracing_courses
    ADD COLUMN status           VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',  -- IN_PROGRESS / COMPLETED / ABANDONED
    ADD COLUMN ended_at         TIMESTAMPTZ,                               -- 종료 시각(COMPLETED 일 때만)
    ADD COLUMN duration_minutes INTEGER     NOT NULL DEFAULT 0,            -- 소요 시간(분)
    ADD COLUMN distance_meters  INTEGER     NOT NULL DEFAULT 0;            -- 이동 거리(m)
ALTER TABLE public.tracing_courses ALTER COLUMN status DROP DEFAULT;
UPDATE public.tracing_courses SET ended_at = created_at;  -- 기존 행은 완주로 보므로 종료 시각 = 생성 시각

-- 진행 중 세션은 (user, course) 당 하나 — 시작 API 가 멱등하게 기존 세션을 돌려준다.
CREATE UNIQUE INDEX tracing_courses_in_progress_key
    ON public.tracing_courses (user_id, course_id) WHERE status = 'IN_PROGRESS';
-- 완주 조회(코스 상세 viewer·저장함 완주 필터) 경로.
CREATE INDEX tracing_courses_user_id_course_id_idx ON public.tracing_courses (user_id, course_id);

-- 따라가기 중 방문(체크)한 장소. 체크인 순서는 id 순이고, 같은 장소 재전송은 (tracing_course_id, place_id) 유니크로 무시한다.
CREATE TABLE public.tracing_course_places (
    id                BIGSERIAL   PRIMARY KEY,
    tracing_course_id BIGINT      NOT NULL,
    place_id          BIGINT      NOT NULL,                  -- 교차 도메인(place): FK 없음
    visited_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT tracing_course_places_tracing_course_id_fkey
        FOREIGN KEY (tracing_course_id) REFERENCES public.tracing_courses(id),
    CONSTRAINT tracing_course_places_tracing_course_id_place_id_key UNIQUE (tracing_course_id, place_id)
);

-- 기능별 최소 빌드 정책 — 따라가기(course-tracing) 키 신설. 따라가기 API 가 들어가는 빌드가 5 라 그 미만은 426.
-- 값 변경은 UPSERT, 롤백은 행 삭제(V9 plan 방식).
INSERT INTO app_version_policies (feature, platform, min_build) VALUES
    ('course-tracing', 'android', 5);
