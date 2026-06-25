-- 초기 스키마. Exposed Samples 테이블과 일치.
CREATE TABLE samples (
    id   SERIAL       PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);
