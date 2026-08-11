package com.example.backend.bootstrap.config

import org.opensearch.client.opensearch.OpenSearchClient
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.health.contributor.Status

/**
 * OpenSearch 연결 상태를 `/actuator/health`에 노출한다(연결 검증용).
 *
 * 도달 시 UP(클러스터명·버전 detail). **불통 시 DOWN 이 아니라 UNKNOWN 을 반환한다** —
 * ALB 타깃그룹이 `/actuator/health`(집계)를 ELB 헬스타입으로 보므로, OpenSearch 일시 불통이
 * 앱 health 를 DOWN 으로 만들어 인스턴스가 교체되는 것을 막는다(UNKNOWN 은 집계상 UP 보다 덜 심각).
 */
class OpenSearchHealthIndicator(
    private val client: OpenSearchClient,
) : HealthIndicator {
    override fun health(): Health =
        try {
            val info = client.info()
            Health
                .up()
                .withDetail("cluster", info.clusterName())
                .withDetail("version", info.version().number())
                .build()
        } catch (e: Exception) {
            Health
                .status(Status.UNKNOWN)
                .withDetail("error", e.message ?: e.javaClass.simpleName)
                .build()
        }
}
