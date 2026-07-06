-- Member 스키마. Exposed MemberTable 과 일치.
CREATE TABLE members (
    id   SERIAL       PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    area VARCHAR(50)  NOT NULL
);
