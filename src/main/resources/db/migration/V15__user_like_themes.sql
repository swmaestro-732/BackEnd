-- 관심 테마(user_like_tags)의 참조 대상을 바로잡는다.
-- V1 은 tag_id 로 course.tags 를 가리켰지만, tags 는 코스 만들 때 자유 입력으로 find-or-create 되는
-- 해시태그 마스터라(시드 없음 · "통창뷰" 같은 사용자 생성 값) 관심 테마의 선택지가 될 수 없다.
-- 정본은 taxonomy.md 의 12종 = CourseCategory enum 이므로, courses.category 와 동일하게
-- enum 이름 문자열로 저장한다(V3 enum 저장 컨벤션 — enumerationByName).
--
-- 기존 행은 자유 태그 id 라 새 모델로 매핑할 수 없어 버린다(관심 테마는 프로필 편집에서 다시 고른다).
DELETE FROM user_like_tags;

ALTER TABLE user_like_tags DROP CONSTRAINT user_like_tags_pkey;
ALTER TABLE user_like_tags DROP COLUMN tag_id;
ALTER TABLE user_like_tags ADD COLUMN category VARCHAR(20) NOT NULL;
ALTER TABLE user_like_tags ADD PRIMARY KEY (user_id, category);
