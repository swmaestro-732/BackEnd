package com.example.backend.bootstrap.config

import io.micrometer.observation.Observation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpMethod
import org.springframework.http.client.observation.ClientRequestObservationContext
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.net.URI

class ObservabilityConfigTest {
    private val predicate = ObservabilityConfig().actuatorObservationPredicate()

    @ParameterizedTest
    @ValueSource(strings = ["/actuator/health", "/actuator/prometheus"])
    fun `Actuator HTTP 관측은 제외한다`(uri: String) {
        val context = ServerRequestObservationContext(MockHttpServletRequest("GET", uri), MockHttpServletResponse())

        assertFalse(predicate.test("http.server.requests", context))
    }

    @Test
    fun `일반 API HTTP 관측은 유지한다`() {
        val context =
            ServerRequestObservationContext(
                MockHttpServletRequest("GET", "/api/v1/places/map"),
                MockHttpServletResponse(),
            )

        assertTrue(predicate.test("http.server.requests", context))
    }

    @Test
    fun `HTTP 이외 관측은 유지한다`() {
        assertTrue(predicate.test("jdbc.query", Observation.Context()))
    }

    @Test
    fun `외부 호출 http_url 에서 쿼리스트링을 지운다`() {
        val request =
            MockClientHttpRequest(
                HttpMethod.GET,
                URI.create(
                    "https://dapi.kakao.com/v2/local/search/keyword.json?query=%EC%84%B1%EC%88%98&x=127.0&y=37.5",
                ),
            )
        val context = ClientRequestObservationContext(request)

        val url =
            ObservabilityConfig()
                .clientRequestObservationConvention()
                .getHighCardinalityKeyValues(context)
                .first { it.key == "http.url" }
                .value

        assertEquals("https://dapi.kakao.com/v2/local/search/keyword.json", url)
    }
}
