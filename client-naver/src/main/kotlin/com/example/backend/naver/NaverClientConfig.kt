package com.example.backend.naver

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * 네이버 API [RestClient] 빈. base URL 과 연결/읽기 타임아웃만 갖는다.
 * 자격증명(키, 토큰)은 굽지 않는다. [NaverProfileClient] 가 요청마다 인증 헤더를 붙인다.
 */
@Configuration
class NaverClientConfig {
    @Bean
    fun naverRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl("https://openapi.naver.com")
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(Duration.ofSeconds(2))
                    setReadTimeout(Duration.ofSeconds(3))
                },
            ).build()
}
