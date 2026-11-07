package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.common.geo.Coordinate
import com.example.backend.course.application.port.inbound.dto.TracePlaceResult
import com.example.backend.course.application.port.inbound.dto.TraceRouteGuideResult
import com.example.backend.course.application.port.inbound.dto.TraceRouteResult
import com.example.backend.course.application.port.inbound.dto.TraceWalkingRouteResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * [TraceRouteResponse.from] 단위 테스트 — 순수 매핑.
 * 도보 가능/불가에 따른 필드 채움과 분 올림을 검증한다.
 */
class TraceRouteResponseTest {
    private val from = TracePlaceResult(22L, "어니언 성수", Coordinate(latitude = 37.5446, longitude = 127.0559))
    private val to = TracePlaceResult(23L, "대림창고", Coordinate(latitude = 37.5420, longitude = 127.0563))

    @Test
    fun `도보 가능하면 거리·초·올림한 분·폴리라인·안내를 채운다`() {
        val mid = Coordinate(latitude = 37.5433, longitude = 127.0561)
        val result =
            TraceRouteResult(
                from = from,
                to = to,
                route =
                    TraceWalkingRouteResult(
                        distanceMeters = 420,
                        seconds = 361,
                        path = listOf(from.coordinate, mid, to.coordinate),
                        guides =
                            listOf(
                                TraceRouteGuideResult(from.coordinate, "출발"),
                                TraceRouteGuideResult(to.coordinate, "도착"),
                            ),
                    ),
            )

        val response = TraceRouteResponse.from(result)

        assertThat(response.from)
            .isEqualTo(TraceRouteResponse.PlacePointResponse(22L, "어니언 성수", 37.5446, 127.0559))
        assertThat(response.to)
            .isEqualTo(TraceRouteResponse.PlacePointResponse(23L, "대림창고", 37.5420, 127.0563))
        assertThat(response.walkable).isTrue()
        assertThat(response.distanceMeters).isEqualTo(420)
        assertThat(response.durationSeconds).isEqualTo(361)
        assertThat(response.durationMinutes).isEqualTo(7) // 361초 → 올림 7분
        assertThat(response.path).containsExactly(
            TraceRouteResponse.PointResponse(37.5446, 127.0559),
            TraceRouteResponse.PointResponse(37.5433, 127.0561),
            TraceRouteResponse.PointResponse(37.5420, 127.0563),
        )
        assertThat(response.guides).containsExactly(
            TraceRouteResponse.GuideResponse(37.5446, 127.0559, "출발"),
            TraceRouteResponse.GuideResponse(37.5420, 127.0563, "도착"),
        )
    }

    @Test
    fun `도보 불가면 walkable false 에 거리·시간은 null, 경로·안내는 빈 배열이다`() {
        val response = TraceRouteResponse.from(TraceRouteResult(from = from, to = to, route = null))

        assertThat(response.walkable).isFalse()
        assertThat(response.distanceMeters).isNull()
        assertThat(response.durationSeconds).isNull()
        assertThat(response.durationMinutes).isNull()
        assertThat(response.path).isEmpty()
        assertThat(response.guides).isEmpty()
        assertThat(response.from.placeId).isEqualTo(22L)
        assertThat(response.to.placeId).isEqualTo(23L)
    }
}
