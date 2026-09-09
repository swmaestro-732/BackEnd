package com.example.backend.bootstrap.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.util.Timeout
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.OpenSearchTransport
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * AWS OpenSearch(VPC 도메인) 클라이언트 배선. `opensearch.endpoint` 가 비어있지 않을 때만 활성(fail-soft) —
 * 로컬·CI(엔드포인트 미주입/빈값)에서는 이 설정 전체가 비활성이라 클라이언트·헬스 인디케이터가 생성되지 않고 앱은 정상 기동한다.
 * (@ConditionalOnProperty 는 빈 문자열도 "존재"로 보므로, 공백 제거 후 길이로 판정한다.)
 * HTTPS(443) + FGAC basic auth. 네트워크 도달은 VPC SG(앱티어→도메인 443)로 이미 허용돼 있다.
 */
@Configuration
@ConditionalOnExpression("'\${opensearch.endpoint:}'.trim().length() > 0")
class OpenSearchConfig(
    private val properties: OpenSearchProperties,
) {
    /**
     * transport 는 커넥션풀·I/O 리액터 스레드를 쥔 [Closeable] 이므로 별도 빈으로 등록해
     * destroyMethod = "close" 로 컨텍스트 종료 시 정리한다(OpenSearchClient 자체는 Closeable 이 아님).
     */
    @Bean(destroyMethod = "close")
    fun openSearchTransport(): OpenSearchTransport {
        // @ConditionalOnExpression 의 유효성 판정과 동일하게 trim 한 값을 써서, 양끝 공백이 있어도 HttpHost 가 정상 생성되게 한다.
        // 프로덕션 시크릿은 스킴 없는 호스트만 담으므로 https:443 으로 붙인다(기존 동작 유지).
        // 스킴이 포함된 경우(예: 로컬/CI Testcontainers http://host:port)는 그대로 파싱해 임의 스킴·포트를 허용한다.
        val endpoint = properties.endpoint.trim()
        val host = if ("://" in endpoint) HttpHost.create(endpoint) else HttpHost("https", endpoint, 443)
        val credentialsProvider =
            BasicCredentialsProvider().apply {
                setCredentials(
                    AuthScope(host),
                    UsernamePasswordCredentials(properties.username, properties.password.toCharArray()),
                )
            }
        // 타임아웃 3종을 모두 ALB 헬스체크 타임아웃보다 짧게(2초) 둔다 — OpenSearch 불통·풀 고갈 시 health() 가
        // 매달리면 /actuator/health 응답이 늦어져 ALB 가 인스턴스를 죽인다. 빠르게 실패시켜 UNKNOWN 으로 떨어지게 한다.
        // (connect=TCP 연결, response=응답 대기, connectionRequest=풀에서 커넥션 대여 대기)
        val requestConfig =
            RequestConfig
                .custom()
                .setConnectTimeout(Timeout.ofSeconds(2))
                .setResponseTimeout(Timeout.ofSeconds(2))
                .setConnectionRequestTimeout(Timeout.ofSeconds(2))
                .build()
        // Kotlin data class(CourseDocument 등)를 검색 응답에서 역직렬화하려면 kotlin 모듈이 필요하다.
        // 색인 문서에 없는 필드가 늘어도 읽기가 깨지지 않도록 알 수 없는 속성은 무시한다.
        val jsonMapper =
            ObjectMapper()
                .registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return ApacheHttpClient5TransportBuilder
            .builder(host)
            .setMapper(JacksonJsonpMapper(jsonMapper))
            .setHttpClientConfigCallback {
                it
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setDefaultRequestConfig(requestConfig)
                    // httpclient5 의 자동 콘텐츠 압축을 끈다 — 켜두면 opensearch-java 가 Content-Encoding: gzip 을 보고
                    // 다시 gunzip 하려다 이중 처리로 ZipException(Not in GZIP format) 이 난다. 꺼서 평문 응답으로 파싱.
                    .disableContentCompression()
            }.build()
    }

    @Bean
    fun openSearchClient(transport: OpenSearchTransport): OpenSearchClient = OpenSearchClient(transport)

    /** 연결 상태를 `/actuator/health` 에 노출(불통 시 UNKNOWN — ALB 보호). 엔드포인트 있을 때만 등록된다. */
    @Bean
    fun openSearchHealthIndicator(openSearchClient: OpenSearchClient): HealthIndicator =
        OpenSearchHealthIndicator(openSearchClient)
}
