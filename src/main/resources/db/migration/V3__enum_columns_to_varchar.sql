-- 앱 레벨 enum 컬럼 저장 방식을 SMALLINT(코드) → VARCHAR(enum 이름)으로 전환한다.
-- 근거: 멘토 리뷰 — 정수 코드는 (1) DB 원본 가독성이 나쁘고 (2) enum 순서/코드 변경 실수가 그대로
--       데이터로 전이된다. Exposed `enumerationByName` 은 이름으로 저장하며, 상수 이름이 바뀌면
--       매핑이 깨지는 것을 코드에서 드러낸다.
-- 대상: Kotlin Table 에 매핑된 enum 컬럼만(계약이 코드에 있는 것). users.status 는 아직 매핑 enum 이
--       없어 SMALLINT 유지 — UserStatus enum 도입 시 함께 전환한다.
-- USING CASE 로 기존 코드값(0..n)을 enum 이름으로 옮기고, DEFAULT 도 이름으로 재설정한다.

-- places.status: ACTIVE(0) HIDDEN(1) SUSPENDED(2) DELETED(3)
ALTER TABLE places
    ALTER COLUMN status DROP DEFAULT,
    ALTER COLUMN status TYPE VARCHAR(32)
        USING CASE status
            WHEN 0 THEN 'ACTIVE'
            WHEN 1 THEN 'HIDDEN'
            WHEN 2 THEN 'SUSPENDED'
            WHEN 3 THEN 'DELETED'
        END,
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- places.business_status: OPEN(0) TEMPORARILY_CLOSED(1) PERMANENTLY_CLOSED(2) UNKNOWN(3)
ALTER TABLE places
    ALTER COLUMN business_status DROP DEFAULT,
    ALTER COLUMN business_status TYPE VARCHAR(32)
        USING CASE business_status
            WHEN 0 THEN 'OPEN'
            WHEN 1 THEN 'TEMPORARILY_CLOSED'
            WHEN 2 THEN 'PERMANENTLY_CLOSED'
            WHEN 3 THEN 'UNKNOWN'
        END,
    ALTER COLUMN business_status SET DEFAULT 'UNKNOWN';

-- place_reviews.status: PUBLISHED(0) HIDDEN(1) DELETED(2)
ALTER TABLE place_reviews
    ALTER COLUMN status DROP DEFAULT,
    ALTER COLUMN status TYPE VARCHAR(32)
        USING CASE status
            WHEN 0 THEN 'PUBLISHED'
            WHEN 1 THEN 'HIDDEN'
            WHEN 2 THEN 'DELETED'
        END,
    ALTER COLUMN status SET DEFAULT 'PUBLISHED';

-- courses.status: ACTIVE(0) HIDDEN(1) SUSPENDED(2) DELETED(3)
ALTER TABLE courses
    ALTER COLUMN status DROP DEFAULT,
    ALTER COLUMN status TYPE VARCHAR(32)
        USING CASE status
            WHEN 0 THEN 'ACTIVE'
            WHEN 1 THEN 'HIDDEN'
            WHEN 2 THEN 'SUSPENDED'
            WHEN 3 THEN 'DELETED'
        END,
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- courses.visibility: PUBLIC(0) FRIENDS(1) PRIVATE(2)
ALTER TABLE courses
    ALTER COLUMN visibility DROP DEFAULT,
    ALTER COLUMN visibility TYPE VARCHAR(32)
        USING CASE visibility
            WHEN 0 THEN 'PUBLIC'
            WHEN 1 THEN 'FRIENDS'
            WHEN 2 THEN 'PRIVATE'
        END,
    ALTER COLUMN visibility SET DEFAULT 'PUBLIC';

-- course_reviews.status: PUBLISHED(0) HIDDEN(1) DELETED(2)
ALTER TABLE course_reviews
    ALTER COLUMN status DROP DEFAULT,
    ALTER COLUMN status TYPE VARCHAR(32)
        USING CASE status
            WHEN 0 THEN 'PUBLISHED'
            WHEN 1 THEN 'HIDDEN'
            WHEN 2 THEN 'DELETED'
        END,
    ALTER COLUMN status SET DEFAULT 'PUBLISHED';
