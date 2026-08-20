package com.example.backend.bootstrap.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * AWS OpenSearch(VPC 도메인) 연결 설정 — FGAC basic auth.
 * 값은 배포 시 env(OPENSEARCH_ENDPOINT/USERNAME/PASSWORD)로 주입한다(Secrets Manager `chilsami/opensearch/master`).
 * [endpoint]가 비면(로컬·CI) 클라이언트 빈을 만들지 않는다(fail-soft) — [OpenSearchConfig]의 @ConditionalOnProperty.
 */
@ConfigurationProperties(prefix = "opensearch")
data class OpenSearchProperties(
    /** VPC 도메인 호스트(스킴 없이, 예: vpc-xxx.ap-northeast-2.es.amazonaws.com). 빈 값이면 미연결. */
    val endpoint: String = "",
    val username: String = "",
    val password: String = "",
    /** 부팅 시 강제 재색인(드리프트 복구). 기본 false. */
    val reindexOnStartup: Boolean = false,
)
