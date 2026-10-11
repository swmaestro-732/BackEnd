-- 신고(SCRUM-587) — 코스·사용자·코스 댓글을 (target_type, target_id) 다형 참조로 한 테이블에 저장한다.
-- 대상 테이블이 도메인마다 달라 FK 는 두지 않는다(교차 도메인 논리 참조 규칙). 존재·소유자 검증은 애플리케이션이 한다.
-- target_type·reason·status 는 enum 이름(대문자)이 DB 저장 계약이다.
-- 같은 신고자가 같은 대상을 중복 신고하지 못하게 유니크 — 서비스 사전검사와 별개로 동시 요청(TOCTOU)도 DB 가 막고,
-- 위반(SQLSTATE 23505)은 GlobalExceptionHandler 가 인덱스명으로 식별해 4099(REPORT_ALREADY_EXISTS)로 변환한다.
CREATE TABLE reports (
    id bigserial PRIMARY KEY,
    reporter_id bigint NOT NULL,
    target_type varchar(32) NOT NULL,
    target_id bigint NOT NULL,
    reason varchar(32) NOT NULL,
    description varchar(500),
    status varchar(32) DEFAULT 'PENDING' NOT NULL,
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL
);

CREATE UNIQUE INDEX uq_reports_reporter_target ON reports (reporter_id, target_type, target_id);
