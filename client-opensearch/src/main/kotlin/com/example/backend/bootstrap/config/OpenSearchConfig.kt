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
import org.opensearch.client.transport.aws.AwsSdk2Transport
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import java.time.Duration

/**
 * AWS OpenSearch(VPC 도메인) 클라이언트 배선. `opensearch.endpoint` 가 비어있지 않을 때만 활성(fail-soft) —
 * 로컬·CI(엔드포인트 미주입/빈값)에서는 이 설정 전체가 비활성이라 클라이언트·헬스 인디케이터가 생성되지 않고 앱은 정상 기동한다.
 * (@ConditionalOnProperty 는 빈 문자열도 "존재"로 보므로, 공백 제거 후 길이로 판정한다.)
 * HTTPS(443) + FGAC basic auth(기본) 또는 IAM/SigV4. 네트워크 도달은 VPC SG(앱티어→도메인 443)로 이미 허용돼 있다.
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
    @Primary
    fun openSearchTransport(): OpenSearchTransport = buildTransport(SEARCH_RESPONSE_TIMEOUT)

    /**
     * 색인용 transport — 부팅 초기화(인덱스 생성), 재색인 bulk, 색인 어댑터가 쓴다. 인덱스 생성(nori 사용자 사전 로딩)과
     * 첫 bulk 는 2초를 넘겨, 서버는 끝냈는데 클라이언트만 timeout 으로 실패 처리하는 어긋남이 있었다(SCRUM-567).
     * 응답 대기만 늘리고 연결·대여 timeout 은 2초로 둬 도메인 불통이면 빨리 실패한다.
     */
    @Bean(destroyMethod = "close")
    fun openSearchIndexTransport(): OpenSearchTransport = buildTransport(INDEX_RESPONSE_TIMEOUT)

    private fun buildTransport(responseTimeout: Duration): OpenSearchTransport {
        // @ConditionalOnExpression 의 유효성 판정과 동일하게 trim 한 값을 써서, 양끝 공백이 있어도 HttpHost 가 정상 생성되게 한다.
        // 프로덕션 시크릿은 스킴 없는 호스트만 담으므로 https:443 으로 붙인다(기존 동작 유지).
        // 스킴이 포함된 경우(예: 로컬/CI Testcontainers http://host:port)는 그대로 파싱해 임의 스킴·포트를 허용한다.
        val endpoint = properties.endpoint.trim()
        val host = if ("://" in endpoint) HttpHost.create(endpoint) else HttpHost("https", endpoint, 443)
        // Kotlin data class(CourseDocument 등)를 검색 응답에서 역직렬화하려면 kotlin 모듈이 필요하다.
        // 색인 문서에 없는 필드가 늘어도 읽기가 깨지지 않도록 알 수 없는 속성은 무시한다.
        val jsonMapper =
            ObjectMapper()
                .registerKotlinModule()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        // authMode 오타가 조용히 basic 으로 폴백해 IAM 전용 도메인에서 런타임 인증 실패로 이어지는 걸 막는다.
        // trim+소문자 정규화 후 basic/iam 이외면 기동을 중단한다("iam "·"SigV4" 등).
        val authMode = properties.authMode.trim().lowercase()
        require(authMode == "basic" || authMode == "iam") {
            "opensearch.auth-mode 는 basic 또는 iam 이어야 합니다: ${properties.authMode}"
        }
        if (authMode == "iam") {
            require(properties.region.isNotBlank()) { "IAM 인증 모드에서는 opensearch.region 설정이 필요합니다." }
            val region = Region.of(properties.region.trim())
            val httpClient =
                ApacheHttpClient
                    .builder()
                    .connectionTimeout(Duration.ofSeconds(2))
                    .socketTimeout(responseTimeout)
                    .connectionAcquisitionTimeout(Duration.ofSeconds(2))
                    .build()
            val credentialsProvider = DefaultCredentialsProvider.builder().build()
            val options =
                AwsSdk2TransportOptions
                    .builder()
                    .setCredentials(credentialsProvider)
                    .setMapper(JacksonJsonpMapper(jsonMapper))
                    .build()
            // AwsSdk2Transport 는 HTTPS 로 연결하므로 스킴·포트를 제외한 호스트명만 넘긴다.
            return object : AwsSdk2Transport(httpClient, host.hostName, "es", region, options) {
                // 2.25.0 의 close() 는 no-op 이므로 직접 만든 리소스를 빈 종료 시 정리한다.
                override fun close() {
                    try {
                        httpClient.close()
                    } finally {
                        credentialsProvider.close()
                    }
                }
            }
        }
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
                .setResponseTimeout(Timeout.of(responseTimeout))
                .setConnectionRequestTimeout(Timeout.ofSeconds(2))
                .build()
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

    /** 검색, 헬스체크용 기본 클라이언트(응답 대기 2초). */
    @Bean
    @Primary
    fun openSearchClient(
        @Qualifier("openSearchTransport") transport: OpenSearchTransport,
    ): OpenSearchClient = OpenSearchClient(transport)

    /** 색인용 클라이언트(응답 대기 [INDEX_RESPONSE_TIMEOUT]). 주입 시 `@Qualifier(OpenSearchConfig.INDEX_CLIENT)`. */
    @Bean(INDEX_CLIENT)
    fun openSearchIndexClient(
        @Qualifier("openSearchIndexTransport") transport: OpenSearchTransport,
    ): OpenSearchClient = OpenSearchClient(transport)

    /** 연결 상태를 `/actuator/health` 에 노출(불통 시 UNKNOWN — ALB 보호). 엔드포인트 있을 때만 등록된다. */
    @Bean
    fun openSearchHealthIndicator(openSearchClient: OpenSearchClient): HealthIndicator =
        OpenSearchHealthIndicator(openSearchClient)

    companion object {
        const val INDEX_CLIENT = "openSearchIndexClient"

        /** 검색, 헬스체크 응답 대기. ALB 헬스체크보다 짧아야 한다(아래 basic 모드 주석 참고). */
        private val SEARCH_RESPONSE_TIMEOUT: Duration = Duration.ofSeconds(2)

        /** 색인 응답 대기. 인덱스 생성, 100건 bulk 가 dev(SigV4)에서 2초를 넘긴 것을 보고 정했다. */
        private val INDEX_RESPONSE_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
