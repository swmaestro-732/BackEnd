-- 릴리스 리뷰(BackEnd#21 CodeRabbit) 반영: 주석으로만 있던 제약을 DB 레벨로 강제한다.

-- 동시성 인덱스 생성 전에 기존 중복을 정리하되, 가장 나중에 적재된 값을 남긴다.
DELETE FROM place_business_hours AS older
USING place_business_hours AS newer
WHERE older.place_id = newer.place_id
  AND older.day_of_week = newer.day_of_week
  AND older.id < newer.id;

-- 영업시간 조회·쓰기를 오래 차단하지 않고 유일 인덱스를 생성한 뒤 제약으로 연결한다.
CREATE UNIQUE INDEX CONCURRENTLY uq_place_business_hours_place_day
    ON place_business_hours (place_id, day_of_week);

ALTER TABLE place_business_hours
    ADD CONSTRAINT uq_place_business_hours_place_day
    UNIQUE USING INDEX uq_place_business_hours_place_day;

-- rating 은 1~5 (기존에는 주석으로만 명시). 범위 밖 값이 저장되면 평균 평점이 조용히 왜곡된다.
ALTER TABLE place_reviews
    ADD CONSTRAINT chk_place_reviews_rating CHECK (rating BETWEEN 1 AND 5);

ALTER TABLE course_reviews
    ADD CONSTRAINT chk_course_reviews_rating CHECK (rating BETWEEN 1 AND 5);
