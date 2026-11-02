package com.example.backend.course.adapter.inbound.web

import com.example.backend.bootstrap.security.JwtTokenProvider
import com.example.backend.common.geo.Coordinate
import com.example.backend.direction.application.port.outbound.PedestrianRoute
import com.example.backend.direction.application.port.outbound.PedestrianRoutePort
import com.example.backend.support.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 따라가기 경로 조회(`GET /api/v1/traces?fromPlaceId&toPlaceId`) 통합 테스트.
 * Tmap 호출([PedestrianRoutePort])만 목으로 바꾸고 장소 조회·direction 변환·응답 매핑은
 * 실제 빈을 탄다.
 * 픽스처(course-trace-fixture.sql): 장소 22(어니언 성수)·23(대림창고), 99 는 없는 장소.
 */
@AutoConfigureMockMvc
@Sql(scripts = ["/sql/course-trace-fixture.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseTraceRouteControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val jwtTokenProvider: JwtTokenProvider,
    ) : IntegrationTestBase() {
        @MockitoBean
        private lateinit var pedestrianRoutePort: PedestrianRoutePort

        private val onion = Coordinate(latitude = 37.5446, longitude = 127.0559)
        private val daelim = Coordinate(latitude = 37.5420, longitude = 127.0563)

        @Test
        fun `도보 가능하면 200 과 거리·올림한 분·폴리라인·안내를 내려준다`() {
            `when`(pedestrianRoutePort.walkingRoute(onion, daelim)).thenReturn(
                PedestrianRoute.Reachable(
                    seconds = 361,
                    distanceMeters = 420,
                    path = listOf(onion, daelim),
                    guides = listOf(PedestrianRoute.Guide(onion, "출발"), PedestrianRoute.Guide(daelim, "도착")),
                ),
            )

            route(22L, 23L)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(2000))
                .andExpect(jsonPath("$.data.from.placeId").value(22))
                .andExpect(jsonPath("$.data.from.name").value("어니언 성수"))
                .andExpect(jsonPath("$.data.from.latitude").value(37.5446))
                .andExpect(jsonPath("$.data.to.placeId").value(23))
                .andExpect(jsonPath("$.data.to.longitude").value(127.0563))
                .andExpect(jsonPath("$.data.walkable").value(true))
                .andExpect(jsonPath("$.data.distanceMeters").value(420))
                .andExpect(jsonPath("$.data.durationSeconds").value(361))
                .andExpect(jsonPath("$.data.durationMinutes").value(7))
                .andExpect(jsonPath("$.data.path.length()").value(2))
                .andExpect(jsonPath("$.data.path[1].latitude").value(37.5420))
                .andExpect(jsonPath("$.data.guides.length()").value(2))
                .andExpect(jsonPath("$.data.guides[0].description").value("출발"))
        }

        @Test
        fun `도보로 갈 수 없는 구간이면 200 과 walkable false 이고 거리·시간은 null 이다`() {
            `when`(pedestrianRoutePort.walkingRoute(onion, daelim)).thenReturn(PedestrianRoute.Unreachable)

            route(22L, 23L)
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.walkable").value(false))
                .andExpect(jsonPath("$.data.distanceMeters").value(null))
                .andExpect(jsonPath("$.data.durationMinutes").value(null))
                .andExpect(jsonPath("$.data.path").isEmpty)
                .andExpect(jsonPath("$.data.guides").isEmpty)
        }

        @Test
        fun `Tmap 호출에 실패하면 503 과 5033 이다(외부 지도 폴백 신호)`() {
            `when`(pedestrianRoutePort.walkingRoute(onion, daelim)).thenReturn(PedestrianRoute.Unknown)

            route(22L, 23L)
                .andExpect(status().isServiceUnavailable)
                .andExpect(jsonPath("$.code").value(5033))
                .andExpect(
                    jsonPath("$.message").value("도보 경로를 불러오지 못했습니다. 외부 지도로 안내합니다."),
                )
        }

        @Test
        fun `출발·도착 장소가 같으면 400 과 4001 이다`() {
            route(22L, 22L)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `없는 장소면 404 와 4043 이다`() {
            route(22L, 99L)
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(4043))
        }

        @Test
        fun `필수 파라미터가 빠지면 400 과 4001 이다`() {
            mockMvc
                .perform(get("/api/v1/traces").param("fromPlaceId", "22").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(4001))
        }

        @Test
        fun `토큰 없으면 401 이다`() {
            mockMvc
                .perform(get("/api/v1/traces").param("fromPlaceId", "22").param("toPlaceId", "23"))
                .andExpect(status().isUnauthorized)
        }

        private fun route(
            fromPlaceId: Long,
            toPlaceId: Long,
        ) = mockMvc.perform(
            get("/api/v1/traces")
                .param("fromPlaceId", fromPlaceId.toString())
                .param("toPlaceId", toPlaceId.toString())
                .header(HttpHeaders.AUTHORIZATION, bearer()),
        )

        private fun bearer() = "Bearer ${jwtTokenProvider.issueAccessToken(USER_ID)}"

        private companion object {
            const val USER_ID = 1L
        }
    }
