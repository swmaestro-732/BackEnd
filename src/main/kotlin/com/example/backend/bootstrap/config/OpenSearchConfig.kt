package com.example.backend.bootstrap.config

import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.core5.http.HttpHost
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * AWS OpenSearch(VPC 도메인) 클라이언트 배선. `opensearch.endpoint` 가 있을 때만 활성(fail-soft) —
 * 로컬·CI(엔드포인트 미주입)에서는 이 설정 전체가 비활성이라 클라이언트·헬스 인디케이터가 생성되지 않고 앱은 정상 기동한다.
 * HTTPS(443) + FGAC basic auth. 네트워크 도달은 VPC SG(앱티어→도메인 443)로 이미 허용돼 있다.
 */
@Configuration
@ConditionalOnProperty(prefix = "opensearch", name = ["endpoint"])
class OpenSearchConfig(
    private val properties: OpenSearchProperties,
) {
    @Bean
    fun openSearchClient(): OpenSearchClient {
        val host = HttpHost("https", properties.endpoint, 443)
        val credentialsProvider =
            BasicCredentialsProvider().apply {
                setCredentials(
                    AuthScope(host),
                    UsernamePasswordCredentials(properties.username, properties.password.toCharArray()),
                )
            }
        val transport =
            ApacheHttpClient5TransportBuilder
                .builder(host)
                .setMapper(JacksonJsonpMapper())
                .setHttpClientConfigCallback { it.setDefaultCredentialsProvider(credentialsProvider) }
                .build()
        return OpenSearchClient(transport)
    }

    /** 연결 상태를 `/actuator/health` 에 노출(불통 시 UNKNOWN — ALB 보호). 엔드포인트 있을 때만 등록된다. */
    @Bean
    fun openSearchHealthIndicator(openSearchClient: OpenSearchClient): HealthIndicator =
        OpenSearchHealthIndicator(openSearchClient)
}
