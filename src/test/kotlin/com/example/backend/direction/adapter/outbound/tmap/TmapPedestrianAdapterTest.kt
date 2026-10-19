package com.example.backend.direction.adapter.outbound.tmap

import com.example.backend.bootstrap.config.TmapProperties
import com.example.backend.common.geo.Coordinate
import com.example.backend.direction.application.port.outbound.PedestrianRoute
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
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
    fun `properties_totalTime 초를 Reachable 로 반환한다`() {
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

        val route = adapter.walkingRoute(from, to)

        server.verify()
        assertEquals(540, (route as PedestrianRoute.Reachable).seconds)
        assertEquals(720, route.distanceMeters)
    }

    @Test
    fun `Point 안내와 LineString 좌표를 폴리라인·안내 지점으로 옮긴다`() {
        val (adapter, server) = adapterWith()
        // 실제 응답처럼 Point(안내)·LineString(선분)이 번갈아 오고, 뒤 선분은 앞 선분의 끝점으로 시작한다.
        val body =
            """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","geometry":{"type":"Point","coordinates":[127.0559,37.5446]},
               "properties":{"totalTime":540,"totalDistance":720,"description":"출발"}},
              {"type":"Feature","properties":{},
               "geometry":{"type":"LineString","coordinates":[[127.0559,37.5446],[127.0561,37.5433]]}},
              {"type":"Feature","geometry":{"type":"Point","coordinates":[127.0561,37.5433]},
               "properties":{"description":"  "}},
              {"type":"Feature","properties":{},
               "geometry":{"type":"LineString","coordinates":[[127.0561,37.5433],[127.0563,37.5420]]}},
              {"type":"Feature","geometry":{"type":"Point","coordinates":[127.0563,37.5420]},
               "properties":{"description":"도착"}}
            ]}
            """.trimIndent()
        server
            .expect(requestTo(containsString("/tmap/routes/pedestrian")))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        val route = adapter.walkingRoute(from, to) as PedestrianRoute.Reachable

        // 선분 경계의 중복 좌표는 한 번만 담는다.
        assertEquals(
            listOf(
                Coordinate(latitude = 37.5446, longitude = 127.0559),
                Coordinate(latitude = 37.5433, longitude = 127.0561),
                Coordinate(latitude = 37.5420, longitude = 127.0563),
            ),
            route.path,
        )
        // 문구가 비어 있는 안내 지점은 제외한다.
        assertEquals(listOf("출발", "도착"), route.guides.map { it.description })
        assertEquals(Coordinate(latitude = 37.5446, longitude = 127.0559), route.guides.first().coordinate)
    }

    @Test
    fun `형식이 어긋난 좌표는 건너뛰고 알 수 없는 geometry 는 무시한다`() {
        val (adapter, server) = adapterWith()
        val body =
            """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","geometry":{"type":"Point","coordinates":[127.0559]},
               "properties":{"totalTime":60,"description":"출발"}},
              {"type":"Feature","properties":{},
               "geometry":{"type":"LineString",
                "coordinates":[[127.0559,37.5446],["x",1],[999,37.5433],[127.0561,37.5433]]}},
              {"type":"Feature","geometry":{"type":"Polygon","coordinates":[[127.0561,37.5433]]},"properties":{}},
              {"type":"Feature","properties":{"description":"geometry 없음"}}
            ]}
            """.trimIndent()
        server
            .expect(requestTo(containsString("/tmap/routes/pedestrian")))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        val route = adapter.walkingRoute(from, to) as PedestrianRoute.Reachable

        assertEquals(60, route.seconds)
        assertEquals(0, route.distanceMeters) // totalDistance 없음 → 0
        assertEquals(
            listOf(
                Coordinate(latitude = 37.5446, longitude = 127.0559),
                Coordinate(latitude = 37.5433, longitude = 127.0561),
            ),
            route.path,
        )
        assertEquals(emptyList<PedestrianRoute.Guide>(), route.guides)
    }

    @Test
    fun `totalTime 이 없으면 Unknown 으로 반환한다`() {
        val (adapter, server) = adapterWith()
        val body = """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"totalDistance":720}}]}"""
        server
            .expect(requestTo(containsString("/tmap/routes/pedestrian")))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        assertEquals(PedestrianRoute.Unknown, adapter.walkingRoute(from, to))
    }

    @Test
    fun `NoServiceArea(3102) 는 Unreachable 로 반환한다`() {
        val (adapter, server) = adapterWith()
        val errorBody =
            """
            {"error":{"id":"400","category":"tmap","code":"3102","message":"NoServiceArea"}}
            """.trimIndent()
        server
            .expect(requestTo(containsString("/tmap/routes/pedestrian")))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).body(errorBody).contentType(MediaType.APPLICATION_JSON))

        assertEquals(PedestrianRoute.Unreachable, adapter.walkingRoute(from, to))
    }

    @Test
    fun `서버 오류는 Unknown 으로 반환한다`() {
        val (adapter, server) = adapterWith()
        server.expect(requestTo(containsString("/tmap/routes/pedestrian"))).andRespond(withServerError())

        assertEquals(PedestrianRoute.Unknown, adapter.walkingRoute(from, to))
    }

    @Test
    fun `features 가 비어 있으면 Unknown 으로 반환한다`() {
        val (adapter, server) = adapterWith()
        server
            .expect(requestTo(containsString("/tmap/routes/pedestrian")))
            .andRespond(withSuccess("""{"type":"FeatureCollection","features":[]}""", MediaType.APPLICATION_JSON))

        assertEquals(PedestrianRoute.Unknown, adapter.walkingRoute(from, to))
    }

    @Test
    fun `appKey 가 비어 있으면 호출하지 않고 Unknown 으로 반환한다`() {
        val (adapter, _) = adapterWith(appKey = "")

        assertEquals(PedestrianRoute.Unknown, adapter.walkingRoute(from, to))
    }
}
