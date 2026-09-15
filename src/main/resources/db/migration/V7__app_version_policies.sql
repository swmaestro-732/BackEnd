-- 기능별 앱 최소 빌드 정책(강제 업데이트). 값 미만 빌드는 426. 행이 없으면 검사 통과(fail-open).
-- 값 변경은 UPSERT, 롤백은 행 삭제 — 배포 없이 즉시 반영(캐시 TTL 내 지연).
CREATE TABLE app_version_policies (
    id         BIGSERIAL PRIMARY KEY,
    feature    VARCHAR(50) NOT NULL,
    platform   VARCHAR(10) NOT NULL,
    min_build  INT         NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_version_policies UNIQUE (feature, platform)
);

-- 초기 정책 시드 — 배포 즉시 적용된다. 헤더(X-App-Build) 미전송 클라이언트는 검사 대상이 아니므로
-- versionCode 4 미만(헤더 없는 구버전) 사용자를 차단하지 않는 출발점이다.
INSERT INTO app_version_policies (feature, platform, min_build) VALUES
    ('auth',          'android', 4),
    ('user-profile',  'android', 4),
    ('user-course',   'android', 4),
    ('user-place',    'android', 4),
    ('course-detail', 'android', 4),
    ('course-create', 'android', 4),
    ('course-review', 'android', 4),
    ('place-detail',  'android', 4),
    ('place-search',  'android', 4),
    ('place-review',  'android', 4),
    ('media',         'android', 4),
    ('area',          'android', 4);
