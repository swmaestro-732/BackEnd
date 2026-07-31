-- 코스 상세 조회 실구현(GET /api/v1/courses/{courseId}) 대비 스키마 보강.

-- courses.cover_image_url: 코스 커버 이미지 URL.
-- 근거: 코스 상세 응답(CourseResponse.coverImageUrl)이 스키마 미반영 상태였다 —
--       장소 이미지(course_place_images)와 독립 관리(커버는 코스 대표 1장).
-- 임시저장(draft) 코스는 커버가 없을 수 있어 nullable(기존 행 백필 없음).
ALTER TABLE courses
    ADD COLUMN cover_image_url TEXT;

-- course_places.walking_time(표시 문자열 "도보 11분") → walking_minutes(INT) 로 교체.
-- 근거: 코스 상세 응답이 walkingMinutesToNext:Int / stats.walkingMinutes:Int(합계) 로 분 단위 정수를
--       요구한다. 표시 문자열은 클라이언트가 조합. V6 에서 추가한 walking_time 은 데이터가 없어 drop.
-- 마지막 장소는 다음 이동이 없으므로 nullable.
ALTER TABLE course_places
    DROP COLUMN walking_time;

ALTER TABLE course_places
    ADD COLUMN walking_minutes INT;
