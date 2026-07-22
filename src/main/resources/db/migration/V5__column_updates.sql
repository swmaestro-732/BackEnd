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

-- course_places.walking_time: 다음 장소까지 도보 소요 시간 표시 텍스트(예: "도보 11분").
-- 근거: 코스 동선 화면의 장소 간 이동 시간 표시 — 서버가 계산해 표시용 문자열로 저장.
-- 코스 마지막 장소는 다음 이동이 없으므로 nullable.
ALTER TABLE course_places
    ADD COLUMN walking_time VARCHAR(50);

-- saved_courses.folder_id nullable 전환: 폴더를 지정하지 않은 코스 저장 허용 —
-- NULL이면 폴더 미분류 상태를 뜻한다.
ALTER TABLE saved_courses
    ALTER COLUMN folder_id DROP NOT NULL;

-- saved_courses.user_id 추가: folder_id가 NULL(폴더 미분류)인 행도 소유자를 식별해야 한다.
-- 같은 도메인(users) 참조이므로 FK를 건다 — 기존 행은 폴더의 user_id로 백필 후 NOT NULL 전환.
ALTER TABLE saved_courses
    ADD COLUMN user_id BIGINT REFERENCES users (id);

UPDATE saved_courses sc
SET user_id = f.user_id
FROM saved_course_folders f
WHERE sc.folder_id = f.id;

ALTER TABLE saved_courses
    ALTER COLUMN user_id SET NOT NULL;

-- courses.tracings_cnt: 코스 트레이싱(따라가기) 횟수 카운터 —
-- 기존 카운터(likes_cnt·comments_cnt·saves_cnt)와 동일하게 0 기본값 비정규화 집계.
ALTER TABLE courses
    ADD COLUMN tracings_cnt INT NOT NULL DEFAULT 0;

-- users.username → handle 이름 변경: 계정 식별용 핸들 의미를 컬럼명에 반영.
ALTER TABLE users
    RENAME COLUMN username TO handle;
