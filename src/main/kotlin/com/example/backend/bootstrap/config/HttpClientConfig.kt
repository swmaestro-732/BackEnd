package com.example.backend.bootstrap.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * 외부 지도 연동용 [RestClient] 빈 정의.
 *
 * 각 클라이언트는 base URL + 연결/읽기 타임아웃만 갖는다. **자격증명(키·시크릿)은 굽지 않는다** —
 * 어댑터가 요청마다 프로퍼티에서 읽어 인증 헤더를 붙인다(빈 키는 어댑터에서 fail-soft 처리).
 */
@Configuration
class HttpClientConfig {
    @Bean
    fun naverRestClient(): RestClient = restClient("https://openapi.naver.com")

    @Bean
    fun kakaoRestClient(): RestClient = restClient("https://dapi.kakao.com")

    @Bean
    fun tmapRestClient(): RestClient = restClient("https://apis.openapi.sk.com")

    private fun restClient(baseUrl: String): RestClient =
        RestClient
            .builder()
            .baseUrl(baseUrl)
            .requestFactory(timeoutFactory())
            .build()

    private fun timeoutFactory(): ClientHttpRequestFactory =
        SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(CONNECT_TIMEOUT)
            setReadTimeout(READ_TIMEOUT)
        }

    private companion object {
        val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(2)
        val READ_TIMEOUT: Duration = Duration.ofSeconds(3)
    }
}
