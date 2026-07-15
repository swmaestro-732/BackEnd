-- 릴리스 리뷰(BackEnd#21 CodeRabbit) 반영: 주석으로만 있던 제약을 DB 레벨로 강제한다.

-- 중복 정리와 UNIQUE 제약 추가 사이에 새 쓰기가 들어오지 않도록 마이그레이션 트랜잭션 동안 쓰기를 잠근다.
LOCK TABLE place_business_hours IN SHARE ROW EXCLUSIVE MODE;

-- 기존 중복은 가장 나중에 적재된 값을 남기고 정리한다.
DELETE FROM place_business_hours AS older
USING place_business_hours AS newer
WHERE older.place_id = newer.place_id
  AND older.day_of_week = newer.day_of_week
  AND older.id < newer.id;

ALTER TABLE place_business_hours
    ADD CONSTRAINT uq_place_business_hours_place_day UNIQUE (place_id, day_of_week);

-- rating 은 1~5 (기존에는 주석으로만 명시). 범위 밖 값이 저장되면 평균 평점이 조용히 왜곡된다.
ALTER TABLE place_reviews
    ADD CONSTRAINT chk_place_reviews_rating CHECK (rating BETWEEN 1 AND 5);

ALTER TABLE course_reviews
    ADD CONSTRAINT chk_course_reviews_rating CHECK (rating BETWEEN 1 AND 5);
