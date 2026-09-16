-- users.status SMALLINT → varchar(32): enum 이름 저장 컨벤션(enumerationByName)에 정합 (SCRUM-514, 멘토 리뷰 반영).
-- 나머지 status/category 컬럼은 이미 varchar 이름 저장인데 users.status 만 SMALLINT 코드로 남아 있어 통일한다.
-- 기존 코드값(UserStatus.code 0~4)을 enum 이름으로 변환한다.
ALTER TABLE users ALTER COLUMN status DROP DEFAULT;

ALTER TABLE users
    ALTER COLUMN status TYPE varchar(32)
    -- 예상 밖 코드(0~4 외)는 ELSE 로 조용히 뭉개지 않는다 — 매칭 실패 시 NULL 이 되고
    -- status 는 NOT NULL 이라 마이그레이션이 실패해 이상 데이터를 드러낸다(CodeRabbit).
    USING CASE status
        WHEN 0 THEN 'ACTIVE'
        WHEN 1 THEN 'SUSPENDED'
        WHEN 2 THEN 'PENDING'
        WHEN 3 THEN 'WITHDRAWN'
        WHEN 4 THEN 'DELETED'
    END;

ALTER TABLE users ALTER COLUMN status SET DEFAULT 'ACTIVE';
