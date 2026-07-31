-- 코스 저장 컨트롤러 통합 테스트 픽스처.
-- RESTART IDENTITY 로 serial id 를 예측 가능하게 고정한다(users 1·2, courses 1~5, folders 1~3, saved_courses 1~4).
TRUNCATE TABLE users, courses, saved_course_folders, saved_courses
    RESTART IDENTITY CASCADE;

-- 사용자: 1 = 저장 주체, 2 = 타인(폴더 소유권 검증용)
INSERT INTO users (nickname) VALUES ('저장주체'), ('타인');

-- 코스 1~5: 저장 대상. existsById 는 deleted_at IS NULL 만 보므로 최소 컬럼만 채운다.
INSERT INTO courses (user_id, title, is_published, visibility)
VALUES
    (1, '코스1', true, 'PUBLIC'),
    (1, '코스2', true, 'PUBLIC'),
    (1, '코스3', true, 'PUBLIC'),
    (1, '코스4', true, 'PUBLIC'),
    (1, '코스5', true, 'PUBLIC');

-- 폴더: 1·2 = 저장 주체(1) 소유, 3 = 타인(2) 소유(소유권 실패 400 검증용).
INSERT INTO saved_course_folders (user_id, name, order_no)
VALUES
    (1, '가고싶다', 0),
    (1, '데이트', 1),
    (2, '타인폴더', 0);

-- 저장 주체(1)의 기존 저장 레코드 4건(최신 저장순 = id 내림차순).
-- 코스1 은 저장하지 않는다 — 저장 성공 테스트 대상.
-- id 1: folder1 / course2, id 2: folder1 / course3, id 3: folder2 / course4, id 4: 미분류 / course5
INSERT INTO saved_courses (user_id, folder_id, course_id)
VALUES
    (1, 1, 2),
    (1, 1, 3),
    (1, 2, 4),
    (1, NULL, 5);
