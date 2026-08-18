-- 장소 저장 중복 방지: (user_id, place_id) 유니크 인덱스 — saved_courses(V1 통합분)와 같은 방식.
-- 서비스의 사전검사(existsSavedPlace)와 별개로, 동시 저장 경합(TOCTOU)에서도
-- 장소당 저장 레코드가 1개임을 DB 가 보장한다.
-- 위반(SQLSTATE 23505)은 GlobalExceptionHandler 가 인덱스명(saved_places)으로 식별해
-- 4094(PLACE_ALREADY_SAVED)로 변환한다.
-- 유니크 제약은 살아있는(deleted_at IS NULL) 행에만 적용한다(partial index).
-- 소프트 삭제된 행이 남아도 같은 장소를 다시 저장할 수 있어야 하기 때문 — 전체 유니크면 재저장이 23505 로 막힌다.
CREATE UNIQUE INDEX uq_saved_places_user_place
    ON saved_places (user_id, place_id)
    WHERE deleted_at IS NULL;
