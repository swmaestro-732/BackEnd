package com.example.backend.bootstrap.config

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.web.client.RestClient

/** 외부 API 클라이언트가 Spring 관리 빌더의 관측 레지스트리를 유지하는지 확인한다(SCRUM-806). */
class HttpClientConfigTest {
    @ParameterizedTest
    @ValueSource(strings = ["kakao", "tmap", "naver", "googlePlaces"])
    fun `외부 API 호출은 http_client_requests 관측을 남긴다`(name: String) {
        val observed = mutableListOf<String>()
        val registry =
            ObservationRegistry.create().apply {
                observationConfig().observationHandler(
                    object : ObservationHandler<Observation.Context> {
                        override fun supportsContext(context: Observation.Context) = true

                        override fun onStart(context: Observation.Context) {
                            observed.add(context.name ?: "")
                        }
                    },
                )
            }
        val config = HttpClientConfig(RestClient.builder().observationRegistry(registry))
        val client =
            when (name) {
                "kakao" -> config.kakaoRestClient()
                "tmap" -> config.tmapRestClient()
                "naver" -> config.naverRestClient()
                else -> config.googlePlacesRestClient()
            }

        // 외부 네트워크 대신 닫힌 로컬 포트로 보낸다. 연결은 실패해도 관측은 시작된다.
        runCatching {
            client
                .get()
                .uri("http://127.0.0.1:1/")
                .retrieve()
                .toBodilessEntity()
        }

        assertEquals(listOf("http.client.requests"), observed)
    }
}
