package com.example.backend.bootstrap.config

import io.micrometer.observation.Observation
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

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
}
