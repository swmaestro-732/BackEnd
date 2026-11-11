package com.example.backend.bootstrap.config

import io.micrometer.common.KeyValue
import io.micrometer.observation.ObservationPredicate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.observation.ClientRequestObservationContext
import org.springframework.http.client.observation.ClientRequestObservationConvention
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention
import org.springframework.http.server.observation.ServerRequestObservationContext

@Configuration
class ObservabilityConfig {
    @Bean
    fun actuatorObservationPredicate(): ObservationPredicate =
        ObservationPredicate { _, context ->
            // SCRUM-806: 헬스 체크와 스크레이프가 트레이스를 채우지 않도록 제외한다.
            context !is ServerRequestObservationContext || context.carrier?.requestURI?.startsWith("/actuator") != true
        }

    /**
     * 외부 API 호출 스팬의 `http.url` 에서 쿼리스트링을 지운다(SCRUM-806).
     * 카카오 키워드 검색 등은 사용자 검색어·좌표를 쿼리로 보내므로, 그대로 두면 Tempo 에 저장된다.
     */
    @Bean
    fun clientRequestObservationConvention(): ClientRequestObservationConvention =
        object : DefaultClientRequestObservationConvention() {
            override fun requestUri(context: ClientRequestObservationContext): KeyValue {
                val keyValue = super.requestUri(context)
                return keyValue.value
                    .substringBefore('?')
                    .let { KeyValue.of(keyValue.key, it) }
            }
        }
}
