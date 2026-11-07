package com.example.backend.kakao

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * 카카오 API [RestClient] 빈. base URL 과 연결/읽기 타임아웃만 갖는다.
 * 자격증명(키)은 굽지 않는다. [KakaoLocalClient] 가 요청마다 프로퍼티에서 읽어 인증 헤더를 붙인다.
 */
@Configuration
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
