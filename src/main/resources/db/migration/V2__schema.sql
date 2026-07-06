-- 칠삼이 초기 스키마 (ERD 반영). Postgres.
-- 규약:
--  * enum 성격 컬럼(status/visibility/message_type 등)은 앱 레벨 enum ↔ SMALLINT 로 저장.
--  * 시각은 TIMESTAMPTZ, 생성/수정은 DEFAULT now().
--  * 크로스 도메인 참조는 FK 를 걸지 않고 id 값만 저장(도메인 경계 유지). 같은 도메인 내부만 FK.
--  * 좌표는 PostGIS geography(Point,4326) + GiST 인덱스.
-- 컬럼명은 ERD 다이어그램 기준이며, 세부는 팀 리뷰 후 조정.

DROP TABLE IF EXISTS samples;

CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================ member (users) ============================

CREATE TABLE users (
    id                BIGSERIAL PRIMARY KEY,
    status            SMALLINT     NOT NULL DEFAULT 0,           -- ACTIVE, SUSPENDED, PENDING, WITHDRAWN, DELETED
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    username          VARCHAR(20),                               -- 로그인 ID
    nickname          VARCHAR(20)  NOT NULL UNIQUE,              -- 유저명
    bio               TEXT,
    age               INT,
    profile_image_url TEXT,
    followers_cnt     INT          NOT NULL DEFAULT 0,
    followings_cnt    INT          NOT NULL DEFAULT 0,
    courses_cnt       INT          NOT NULL DEFAULT 0
);

CREATE TABLE user_areas (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    area       VARCHAR(20) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE follows (
    id           BIGSERIAL PRIMARY KEY,
    follower_id  BIGINT      NOT NULL REFERENCES users (id),
    following_id BIGINT      NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (follower_id, following_id)
);

CREATE TABLE user_like_tags (
    user_id BIGINT NOT NULL REFERENCES users (id),
    tag_id  BIGINT NOT NULL,                                     -- cross-domain(course.tags): FK 없음
    PRIMARY KEY (user_id, tag_id)
);

CREATE TABLE saved_course_folders (
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT       NOT NULL REFERENCES users (id),
    name     VARCHAR(200) NOT NULL,
    order_no SMALLINT     NOT NULL DEFAULT 0
);

CREATE TABLE saved_courses (
    id         BIGSERIAL PRIMARY KEY,
    folder_id  BIGINT      NOT NULL REFERENCES saved_course_folders (id),
    course_id  BIGINT      NOT NULL,                             -- cross-domain(course): FK 없음
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE saved_places (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    place_id   BIGINT      NOT NULL,                             -- cross-domain(place): FK 없음
    category   VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE tracing_courses (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    course_id  BIGINT      NOT NULL,                             -- cross-domain(course): FK 없음
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE added_places (
    id                BIGSERIAL PRIMARY KEY,
    tracing_course_id BIGINT      NOT NULL REFERENCES tracing_courses (id),
    place_id          BIGINT      NOT NULL,                      -- cross-domain(place): FK 없음
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE items (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE user_items (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id),
    item_id     BIGINT      NOT NULL REFERENCES items (id),
    item_cnt    INT         NOT NULL DEFAULT 1,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expired_at  TIMESTAMPTZ
);

-- ============================ place ============================

CREATE TABLE places (
    id              BIGSERIAL PRIMARY KEY,
    status          SMALLINT              NOT NULL DEFAULT 0,    -- ACTIVE, HIDDEN, SUSPENDED, DELETED
    created_at      TIMESTAMPTZ           NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ           NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    name            VARCHAR(200)          NOT NULL,
    description     TEXT,
    category        VARCHAR(50)           NOT NULL,
    location        geography(Point,4326) NOT NULL,             -- GiST 반경 검색
    address         VARCHAR(255)          NOT NULL,
    image_url       TEXT,
    business_status SMALLINT              NOT NULL DEFAULT 3     -- OPEN, TEMPORARILY_CLOSED, PERMANENTLY_CLOSED, UNKNOWN
);
CREATE INDEX idx_places_location ON places USING GIST (location);

CREATE TABLE place_business_hours (
    id          BIGSERIAL PRIMARY KEY,
    place_id    BIGINT   NOT NULL REFERENCES places (id),
    day_of_week SMALLINT NOT NULL,
    open_time   TIME,
    close_time  TIME
);

CREATE TABLE place_reviews (
    id         BIGSERIAL PRIMARY KEY,
    place_id   BIGINT      NOT NULL REFERENCES places (id),
    status     SMALLINT    NOT NULL DEFAULT 0,                  -- PUBLISHED, HIDDEN, DELETED
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    user_id    BIGINT      NOT NULL,                            -- cross-domain(member): FK 없음
    rating     SMALLINT    NOT NULL,                            -- 1~5
    content    TEXT
);

CREATE TABLE place_review_photos (
    id              BIGSERIAL PRIMARY KEY,
    place_review_id BIGINT   NOT NULL REFERENCES place_reviews (id),
    image_url       TEXT,
    order_no        SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE place_review_tags (
    id    BIGSERIAL PRIMARY KEY,
    label VARCHAR(20) NOT NULL,                                 -- '커피 맛있어요' 등
    icon  VARCHAR(20) NOT NULL
);

CREATE TABLE place_review_tag_links (
    place_review_id     BIGINT NOT NULL REFERENCES place_reviews (id),
    place_review_tag_id BIGINT NOT NULL REFERENCES place_review_tags (id),
    PRIMARY KEY (place_review_id, place_review_tag_id)
);

-- ============================ course ============================

CREATE TABLE tags (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE courses (
    id             BIGSERIAL PRIMARY KEY,
    status         SMALLINT     NOT NULL DEFAULT 0,             -- ACTIVE, HIDDEN, SUSPENDED, DELETED
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    user_id        BIGINT       NOT NULL,                       -- cross-domain(member): FK 없음
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    area           VARCHAR(100),
    visit_date     DATE,
    is_published   BOOLEAN      NOT NULL DEFAULT false,
    visibility     SMALLINT     NOT NULL DEFAULT 0,             -- PUBLIC, FRIENDS, PRIVATE
    likes_cnt      INT          NOT NULL DEFAULT 0,
    comments_cnt   INT          NOT NULL DEFAULT 0,
    saves_cnt      INT          NOT NULL DEFAULT 0,
    forked_from_id BIGINT                                       -- 포크 원본 course (같은 도메인, nullable)
);

CREATE TABLE course_tags (
    course_id BIGINT NOT NULL REFERENCES courses (id),
    tag_id    BIGINT NOT NULL REFERENCES tags (id),
    PRIMARY KEY (course_id, tag_id)
);

CREATE TABLE course_places (
    id         BIGSERIAL PRIMARY KEY,
    course_id  BIGINT       NOT NULL REFERENCES courses (id),
    place_id   BIGINT       NOT NULL,                           -- cross-domain(place): FK 없음
    order_no   SMALLINT     NOT NULL DEFAULT 0,
    caption    VARCHAR(200),
    subcaption TEXT,
    UNIQUE (course_id, order_no)
);

CREATE TABLE course_places_images (
    id               BIGSERIAL PRIMARY KEY,
    course_places_id BIGINT   NOT NULL REFERENCES course_places (id),
    image_url        TEXT,
    order_no         SMALLINT,
    UNIQUE (course_places_id, order_no)
);

CREATE TABLE course_reviews (
    id         BIGSERIAL PRIMARY KEY,
    course_id  BIGINT      NOT NULL REFERENCES courses (id),
    status     SMALLINT    NOT NULL DEFAULT 0,                  -- PUBLISHED, HIDDEN, DELETED
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    user_id    BIGINT      NOT NULL,                            -- cross-domain(member): FK 없음
    rating     SMALLINT    NOT NULL,                            -- 1~5
    content    TEXT
);

CREATE TABLE course_review_photos (
    id               BIGSERIAL PRIMARY KEY,
    course_review_id BIGINT   NOT NULL REFERENCES course_reviews (id),
    image_url        TEXT,
    order_no         SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE course_review_tags (
    id    BIGSERIAL PRIMARY KEY,
    label VARCHAR(20) NOT NULL,                                 -- '동선이 좋아요' 등
    icon  VARCHAR(20) NOT NULL
);

CREATE TABLE course_review_tag_links (
    course_review_id     BIGINT NOT NULL REFERENCES course_reviews (id),
    course_review_tag_id BIGINT NOT NULL REFERENCES course_review_tags (id),
    PRIMARY KEY (course_review_id, course_review_tag_id)
);

-- ============================ support / chat ============================

CREATE TABLE reports (
    id           BIGSERIAL PRIMARY KEY,
    target_type  SMALLINT    NOT NULL,                          -- COURSE, USER, COMMENT, CHAT
    target_id    BIGINT      NOT NULL,                          -- cross-domain 대상: FK 없음
    description  TEXT        NOT NULL,
    status       SMALLINT    NOT NULL DEFAULT 0,                -- PENDING, REVIEWING, RESOLVED, REJECTED
    processed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE inquiries (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,                                         -- cross-domain(member): FK 없음
    category    VARCHAR(50),
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    status      SMALLINT     NOT NULL DEFAULT 0,                -- PENDING, ANSWERED, CLOSED
    answer      TEXT,
    answered_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE chat_rooms (
    id               BIGSERIAL PRIMARY KEY,
    h3_idx           VARCHAR(20)  NOT NULL UNIQUE,              -- h3 지역 셀
    name             VARCHAR(200),
    active_users_cnt INT          NOT NULL DEFAULT 0
);

CREATE TABLE chat_messages (
    id           BIGSERIAL PRIMARY KEY,
    chat_room_id BIGINT      NOT NULL REFERENCES chat_rooms (id),
    user_id      BIGINT      NOT NULL,                          -- cross-domain(member): FK 없음
    age_group    SMALLINT,                                      -- 0,10,20,...,60+
    message_type SMALLINT    NOT NULL DEFAULT 0,                -- TEXT, PHOTO, VIDEO, VOICE, LOCATION, GAME, FILE, LINK
    content      TEXT,
    media        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
