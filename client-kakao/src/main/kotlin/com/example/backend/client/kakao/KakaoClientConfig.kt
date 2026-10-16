package com.example.backend.client.kakao

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

/** 자격증명(REST 키)은 굽지 않는다 — 클라이언트가 요청마다 붙인다. */
@Configuration
@EnableConfigurationProperties(KakaoLocalProperties::class)
class KakaoClientConfig {
    @Bean
    fun kakaoRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl("https://dapi.kakao.com")
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(Duration.ofSeconds(2))
                    setReadTimeout(Duration.ofSeconds(3))
                },
            ).build()
}
