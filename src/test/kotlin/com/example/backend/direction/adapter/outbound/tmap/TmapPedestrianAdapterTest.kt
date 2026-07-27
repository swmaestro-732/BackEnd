package com.example.backend.direction.adapter.outbound.tmap

import com.example.backend.bootstrap.config.TmapProperties
import com.example.backend.common.geo.Coordinate
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class TmapPedestrianAdapterTest {
    private val from = Coordinate(latitude = 37.5445, longitude = 127.0578)
    private val to = Coordinate(latitude = 37.5432, longitude = 127.0561)

    private fun adapterWith(appKey: String = "app-key"): Pair<TmapPedestrianAdapter, MockRestServiceServer> {
        val builder = RestClient.builder().baseUrl("https://apis.openapi.sk.com")
        val server = MockRestServiceServer.bindTo(builder).build()
        val adapter =
            TmapPedestrianAdapter(
                tmapRestClient = builder.build(),
                tmapProperties = TmapProperties(appKey = appKey),
            )
        return adapter to server
    }

    @Test
    fun `properties_totalTime 초를 반환한다`() {
        val (adapter, server) = adapterWith()
        val body =
            """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","properties":{"totalTime":540,"totalDistance":720}}
            ]}
            """.trimIndent()
        server
            .expect(requestTo(containsString("/tmap/routes/pedestrian")))
            .andExpect(header("appKey", "app-key"))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        val seconds = adapter.walkingSeconds(from, to)

        server.verify()
        assertEquals(540, seconds)
    }

    @Test
    fun `호출이 실패하면 null 을 반환한다`() {
        val (adapter, server) = adapterWith()
        server.expect(requestTo(containsString("/tmap/routes/pedestrian"))).andRespond(withServerError())

        assertNull(adapter.walkingSeconds(from, to))
    }

    @Test
    fun `features 가 비어 있으면 null 을 반환한다`() {
        val (adapter, server) = adapterWith()
        server
            .expect(requestTo(containsString("/tmap/routes/pedestrian")))
            .andRespond(withSuccess("""{"type":"FeatureCollection","features":[]}""", MediaType.APPLICATION_JSON))

        assertNull(adapter.walkingSeconds(from, to))
    }

    @Test
    fun `appKey 가 비어 있으면 호출하지 않고 null 을 반환한다`() {
        val (adapter, _) = adapterWith(appKey = "")

        assertNull(adapter.walkingSeconds(from, to))
    }
}
