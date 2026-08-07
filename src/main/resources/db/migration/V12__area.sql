-- 행정구역(법정동) 도입 — ① area 마스터 테이블 ② course·place·user 의 area_code 참조 컬럼.
--
-- 저장 단위는 읍/면/동(법정동)이고, 시/군/구는 법정동코드 앞 5자리로 매칭한다.
-- 법정동코드 10자리: 앞 2자리=시도, 앞 5자리=시군구, 전체=읍면동
--   예) 1168010100 → 11(서울) / 11680(강남구) / 역삼동
-- 여러 애그리거트(course·place·user)가 code 값(area_code)으로만 참조하며, area 에는 FK 를 걸지 않는다(도메인 경계 유지).

-- ============================ ① area 마스터 ============================

CREATE TABLE area (
    code         VARCHAR(10) PRIMARY KEY,                      -- 법정동코드(문자 10자리). CHAR 는 공백 패딩 이슈로 VARCHAR.
    sido_name    VARCHAR(20) NOT NULL,
    sigungu_name VARCHAR(30),                                  -- 세종시는 시군구 레벨이 없어 NULL 허용
    dong_name    VARCHAR(40) NOT NULL,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,            -- 폐지 코드는 삭제하지 않고 false 로 둔다
    -- 조회 최적화용 파생 컬럼(앞 5자리 = 시군구코드). DB 가 자동 계산하므로 INSERT/UPDATE 에 값을 넣지 않는다.
    sigungu_code VARCHAR(5)  GENERATED ALWAYS AS (SUBSTRING(code, 1, 5)) STORED,
    CONSTRAINT chk_area_code_numeric CHECK (code ~ '^[0-9]{10}$')
);

CREATE INDEX idx_area_sigungu_code_dong_name ON area (sigungu_code, dong_name);
CREATE INDEX idx_area_dong_name              ON area (dong_name);

-- ============================ ② 참조 컬럼(area_code) ============================
-- 기존 행이 이미 있어 우선 전부 NULLABLE 로 추가한다(NOT NULL 즉시 추가 시 기존 행 때문에 마이그레이션 실패).
-- 시드/백필이 준비되면 course·place 의 area_code 는 별도 마이그레이션에서 NOT NULL 로 승격한다(users 는 계속 NULL 허용).
-- sigungu_code 는 조회 최적화용 파생 컬럼(area_code 앞 5자리) — DB 가 자동 계산한다(INSERT/UPDATE 에 넣지 않음).
-- 레거시 자유텍스트 중 courses.area 는 이번 변경에서 건드리지 않고 공존시킨다(user_areas 는 아래 ③에서 전환).

-- ── course ──
ALTER TABLE courses ADD COLUMN area_code    VARCHAR(10);
ALTER TABLE courses ADD COLUMN sigungu_code VARCHAR(5) GENERATED ALWAYS AS (SUBSTRING(area_code, 1, 5)) STORED;
CREATE INDEX idx_courses_area_code    ON courses (area_code);
CREATE INDEX idx_courses_sigungu_code ON courses (sigungu_code);

-- ── place ──
ALTER TABLE places ADD COLUMN area_code    VARCHAR(10);
ALTER TABLE places ADD COLUMN sigungu_code VARCHAR(5) GENERATED ALWAYS AS (SUBSTRING(area_code, 1, 5)) STORED;
CREATE INDEX idx_places_area_code    ON places (area_code);
CREATE INDEX idx_places_sigungu_code ON places (sigungu_code);

-- ── user ──
ALTER TABLE users ADD COLUMN area_code    VARCHAR(10);
ALTER TABLE users ADD COLUMN sigungu_code VARCHAR(5) GENERATED ALWAYS AS (SUBSTRING(area_code, 1, 5)) STORED;
CREATE INDEX idx_users_area_code    ON users (area_code);
CREATE INDEX idx_users_sigungu_code ON users (sigungu_code);

-- ============================ ③ user_areas(관심 지역) 전환 ============================
-- 회원가입 관심 지역을 법정동코드로 저장하도록 레거시 자유텍스트 컬럼(area VARCHAR(20))을 전환한다.
-- 자유텍스트 값은 코드로 변환할 수 없고 읽는 코드도 없었으므로 행을 비우고 형식을 바꾼다.
-- area 마스터에는 FK 를 걸지 않는다(위와 동일 — 존재 검증은 애플리케이션이 한다).

TRUNCATE TABLE user_areas;

ALTER TABLE user_areas RENAME COLUMN area TO area_code;
ALTER TABLE user_areas ALTER COLUMN area_code TYPE VARCHAR(10);

-- 같은 지역 중복 등록 방지. 서비스의 사전 dedup(distinct)과 별개로 DB 가 최종 보장한다.
ALTER TABLE user_areas ADD CONSTRAINT uq_user_areas_user_area_code UNIQUE (user_id, area_code);
