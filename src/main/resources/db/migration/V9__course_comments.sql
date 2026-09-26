CREATE TABLE course_comments (
    id bigserial PRIMARY KEY,
    course_id bigint NOT NULL REFERENCES courses(id),
    user_id bigint NOT NULL,
    content text NOT NULL,
    status varchar(32) DEFAULT 'ACTIVE' NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    deleted_at timestamptz
);

CREATE INDEX idx_course_comments_list ON course_comments (course_id, deleted_at, id);
