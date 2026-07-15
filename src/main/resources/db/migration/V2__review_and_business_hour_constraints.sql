-- 릴리스 리뷰(BackEnd#21 CodeRabbit) 반영: 주석으로만 있던 제약을 DB 레벨로 강제한다.

-- 같은 장소·요일에 영업시간이 중복 적재되면 "매일 HH:mm – HH:mm" 표기 로직의 전제가 깨진다.
ALTER TABLE place_business_hours
    ADD CONSTRAINT uq_place_business_hours_place_day UNIQUE (place_id, day_of_week);

-- rating 은 1~5 (기존에는 주석으로만 명시). 범위 밖 값이 저장되면 평균 평점이 조용히 왜곡된다.
ALTER TABLE place_reviews
    ADD CONSTRAINT chk_place_reviews_rating CHECK (rating BETWEEN 1 AND 5);

ALTER TABLE course_reviews
    ADD CONSTRAINT chk_course_reviews_rating CHECK (rating BETWEEN 1 AND 5);
