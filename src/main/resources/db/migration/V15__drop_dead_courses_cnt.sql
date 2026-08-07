-- 코스 개수를 공개범위별 저장 카운터(V14: public/follower/private_courses_cnt)로 대체하면서,
-- 더 이상 유지되지 않던 단일 총합 컬럼(courses_cnt)을 제거한다. 응답의 coursesCnt 는 세 버킷 합으로 계산한다.
ALTER TABLE users DROP COLUMN courses_cnt;
