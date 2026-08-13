-- 관심 테마의 참조 대상을 바로잡고 테이블 이름을 실제 내용에 맞춘다.
-- V1 은 user_like_tags.tag_id 로 course.tags 를 가리켰지만, tags 는 코스 만들 때 자유 입력으로
-- find-or-create 되는 해시태그 마스터라(시드 없음 · "통창뷰" 같은 사용자 생성 값) 관심 테마의 선택지가 될 수 없다.
-- 정본은 taxonomy.md 의 12종 = CourseCategory enum 이므로, courses.category 와 동일하게
-- enum 이름 문자열로 저장한다(V3 enum 저장 컨벤션 — enumerationByName).
-- 이름에 tags 가 남아 있으면 다시 tags 참조로 오해되므로 테이블도 user_like_categories 로 바꾼다.
--
-- 기존 행은 자유 태그 id 라 새 모델로 매핑할 수 없어 버린다(관심 테마는 프로필 편집에서 다시 고른다).
DELETE FROM user_like_tags;

ALTER TABLE user_like_tags RENAME TO user_like_categories;

-- 테이블을 rename 해도 제약 이름은 따라오지 않는다 — PK 는 재생성하며 새 이름을 얻고(user_like_categories_pkey),
-- FK 는 그대로 남으므로 명시적으로 rename 한다.
ALTER TABLE user_like_categories RENAME CONSTRAINT user_like_tags_user_id_fkey TO user_like_categories_user_id_fkey;

ALTER TABLE user_like_categories DROP CONSTRAINT user_like_tags_pkey;
ALTER TABLE user_like_categories DROP COLUMN tag_id;
ALTER TABLE user_like_categories ADD COLUMN category VARCHAR(20) NOT NULL;
ALTER TABLE user_like_categories ADD PRIMARY KEY (user_id, category);
