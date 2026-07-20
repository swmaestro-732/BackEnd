-- 컬럼 보강 모음 — API 계약 대비 스키마 미반영 컬럼들을 추가한다.

-- saved_places.visited: 방문 여부.
-- 근거: 저장함 미방문/방문 탭(디자인)과 저장 장소 방문 처리 API(PATCH /api/v1/users/saved-places/{placeId}).
--       저장 장소 조회(SCRUM-336) 응답의 visited 필드가 스키마 미반영 상태였던 것을 해소 —
--       방문 처리 전까지는 미방문이므로 기본값 FALSE.
ALTER TABLE saved_places
    ADD COLUMN visited BOOLEAN NOT NULL DEFAULT FALSE;

-- course_places.subcaption 삭제: 장소별 안내 문구는 caption 하나로 충분해 계약에서 제외 —
-- 코스 상세 응답(CoursePlaceResult/Response)에서도 함께 제거한다.
ALTER TABLE course_places
    DROP COLUMN subcaption;

-- courses.category: 코스 카테고리 추가 — places.category 처럼 앱 레벨 enum(CourseCategory) 이름으로 저장.
-- 값: DATE HEALING FOOD CAFETOUR CULTURE NATURE NIGHTVIEW SHOPPING TRADITION ACTIVITY FAMILY SOLO
-- 카테고리 미선택 임시저장(draft) 코스가 있어 nullable(기존 행 백필 없음).
ALTER TABLE courses
    ADD COLUMN category VARCHAR(50);
