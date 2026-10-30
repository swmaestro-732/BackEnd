-- 코스 다중 카테고리 (SCRUM-707). 코스당 1개였던 courses.category 를 조인 테이블로 옮겨 여러 개를 고를 수 있게 한다.
-- 카테고리는 장소에서 도출하지 않고 작성자가 직접 고른다. 값은 CourseCategory enum 이름(varchar 저장 컨벤션).
CREATE TABLE public.course_categories (
    course_id BIGINT      NOT NULL,
    category  VARCHAR(50) NOT NULL,
    CONSTRAINT course_categories_pkey PRIMARY KEY (course_id, category),
    CONSTRAINT course_categories_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id)
);
-- 카테고리로 코스를 거르는 경로(category → course_id).
CREATE INDEX course_categories_category_course_id_idx ON public.course_categories (category, course_id);

-- 기존 단일 카테고리를 그대로 옮긴다. courses.category 컬럼은 배포 중 구버전 호환을 위해 남기고 후속 마이그레이션에서 지운다.
INSERT INTO public.course_categories (course_id, category)
SELECT id, category FROM public.courses WHERE category IS NOT NULL;
