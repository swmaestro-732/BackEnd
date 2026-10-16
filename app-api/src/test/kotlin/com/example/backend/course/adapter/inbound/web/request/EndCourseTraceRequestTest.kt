package com.example.backend.course.adapter.inbound.web.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class EndCourseTraceRequestTest {
    @Test
    fun `소요 시간·이동 거리만 넘기면 visitedPlaces 는 빈 목록이다`() {
        val request = EndCourseTraceRequest(durationMinutes = 0, distanceMeters = 0)

        assertThat(request.visitedPlaces).isEmpty()
        assertThat(request.durationMinutes).isZero()
        assertThat(request.distanceMeters).isZero()
    }

    @Test
    fun `방문 장소를 순서대로 담은 요청을 생성할 수 있다`() {
        val first = EndCourseTraceRequest.VisitedPlace(placeId = 22, visitedAt = Instant.parse("2026-10-10T05:00:00Z"))
        val last = EndCourseTraceRequest.VisitedPlace(placeId = 25, visitedAt = Instant.parse("2026-10-10T07:48:00Z"))

        val request =
            EndCourseTraceRequest(visitedPlaces = listOf(first, last), durationMinutes = 192, distanceMeters = 2400)

        assertThat(request.visitedPlaces).containsExactly(first, last)
        assertThat(request.visitedPlaces.last().placeId).isEqualTo(25)
        assertThat(request.visitedPlaces.last().visitedAt).isEqualTo(Instant.parse("2026-10-10T07:48:00Z"))
        assertThat(request.durationMinutes).isEqualTo(192)
        assertThat(request.distanceMeters).isEqualTo(2400)
    }
}
