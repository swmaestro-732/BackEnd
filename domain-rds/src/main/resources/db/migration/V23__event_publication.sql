-- Spring Modulith 이벤트 저장소(아웃박스) 테이블. spring-modulith-events-jdbc 2.1.2 jar 의
-- org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql 을 그대로 옮겼다(스키마 자동 생성은 끄고 Flyway 가 소유).
-- 커밋 후 리스너마다 한 행이 업무 변경과 같은 트랜잭션으로 쌓이고, 리스너가 끝나면 지워진다(completion-mode: delete).
CREATE TABLE IF NOT EXISTS event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);
