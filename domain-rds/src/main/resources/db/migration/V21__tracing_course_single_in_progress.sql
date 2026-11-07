-- 따라가기는 사용자당 한 코스만 진행한다 (SCRUM-738). 여러 코스를 진행 중인 사용자는 가장 최근 세션만 남기고 포기 처리한다.
UPDATE public.tracing_courses t
SET status = 'ABANDONED'
WHERE t.status = 'IN_PROGRESS'
  AND t.id <> (
      SELECT max(latest.id)
      FROM public.tracing_courses latest
      WHERE latest.user_id = t.user_id
        AND latest.status = 'IN_PROGRESS'
  );

DROP INDEX public.tracing_courses_in_progress_key;
CREATE UNIQUE INDEX tracing_courses_in_progress_key
    ON public.tracing_courses (user_id) WHERE status = 'IN_PROGRESS';
