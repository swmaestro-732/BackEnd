CREATE TABLE public.processed_course_count_events (
    event_id     varchar(64) PRIMARY KEY,
    processed_at timestamp with time zone NOT NULL DEFAULT now()
);
