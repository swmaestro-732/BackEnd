package com.example.backend.crawl.application.service

import com.example.backend.common.geo.Coordinate
import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand
import com.example.backend.crawl.application.port.inbound.dto.CrawledPlaceCommand
import com.example.backend.crawl.application.port.outbound.CrawlCoursePort
import com.example.backend.crawl.application.port.outbound.CrawlImportPersistencePort
import com.example.backend.crawl.application.port.outbound.CrawlPlacePort
import com.example.backend.crawl.application.port.outbound.CrawlWalkingPort
import com.example.backend.crawl.application.port.outbound.CuratedCourseCommand
import com.example.backend.crawl.application.port.outbound.ResolvedCrawlPlace
import com.example.backend.crawl.domain.model.CrawlImport
import com.example.backend.crawl.domain.model.CrawlImportStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.transaction.support.TransactionOperations

class CrawlImportServiceTest {
    private val persistence = FakePersistence()
    private val places = FakePlaces()
    private val walking = FakeWalking()
    private val courses = FakeCourses()
    private val service =
        CrawlImportService(persistence, places, walking, courses, TransactionOperations.withoutTransaction())

    @ParameterizedTest
    @EnumSource(CrawlImportStatus::class, names = ["IMPORTED", "REJECTED"])
    fun `이미 완료한 게시물은 다른 실행에서 재전송해도 처리하지 않는다`(status: CrawlImportStatus) {
        val previous = CrawlImport("C9xYz", "previous-run", status, courseId = 91L)
        persistence.rows[previous.shortcode] = previous

        service.importResult(command())

        assertEquals(previous, persistence.rows["C9xYz"])
        assertTrue(places.requests.isEmpty())
        assertTrue(walking.requests.isEmpty())
        assertTrue(courses.commands.isEmpty())
        assertTrue(persistence.saved.isEmpty())
    }

    @Test
    fun `코스가 아닌 게시물은 거절하고 장소를 등록하지 않는다`() {
        service.importResult(command().copy(isCourse = false))

        assertRejected("not course")
        assertTrue(places.requests.isEmpty())
    }

    @Test
    fun `카카오 ID가 있는 장소가 두 곳 미만이면 거절한다`() {
        service.importResult(command().copy(places = listOf(place(1), place(2).copy(kakaoPlaceId = "  "))))

        assertRejected("fewer than 2 places")
        assertTrue(places.requests.isEmpty())
    }

    @Test
    fun `장소가 없어도 거절 이력을 남긴다`() {
        service.importResult(command().copy(places = emptyList()))

        assertRejected("fewer than 2 places")
    }

    @Test
    fun `실패한 게시물은 다시 처리하고 새 실행의 코스 ID를 기록한다`() {
        persistence.rows["C9xYz"] = CrawlImport("C9xYz", "previous-run", CrawlImportStatus.FAILED, reason = "timeout")

        service.importResult(command())

        assertEquals(1, courses.commands.size)
        assertEquals(
            CrawlImport("C9xYz", "20261027-0400", CrawlImportStatus.IMPORTED, courseId = 91L),
            persistence.rows["C9xYz"],
        )
    }

    @Test
    fun `장소 순서와 저장된 좌표로 경로를 구하고 첫 이미지를 표지로 사용한다`() {
        val first = place(1)
        val second =
            place(2).copy(imageUrls = listOf("https://cdn.example.com/cover.jpg", "https://cdn.example.com/second.jpg"))
        val third = place(3).copy(imageUrls = listOf("https://cdn.example.com/third.jpg"))
        walking.segments = listOf(7, -1)

        service.importResult(
            command().copy(places = listOf(third, place(4).copy(kakaoPlaceId = ""), second, first)),
        )

        val course = courses.commands.single()
        assertEquals("성수 데이트", course.title)
        assertEquals("카페와 산책을 함께 즐기는 코스", course.description)
        assertEquals(listOf("데이트"), course.tags)
        assertEquals("https://cdn.example.com/1.jpg", course.coverImageUrl)
        assertEquals(listOf(101L, 102L, 103L), course.places.map { it.placeId })
        assertEquals(listOf(1, 2, 3), course.places.map { it.orderNo })
        assertEquals(listOf(7, -1, null), course.places.map { it.walkingMinutes })
        assertEquals(second.imageUrls, course.places[1].imageUrls)
        assertEquals(listOf(first, second, third), places.requests.single())
        assertEquals(
            listOf(Coordinate(37.61, 127.1), Coordinate(37.62, 127.1), Coordinate(37.63, 127.1)),
            walking.requests.single(),
        )
        assertEquals(
            CrawlImport("C9xYz", "20261027-0400", CrawlImportStatus.IMPORTED, courseId = 91L),
            persistence.rows["C9xYz"],
        )
    }

    @Test
    fun `사진이 없는 장소가 있으면 발행 규칙을 우회하지 않고 거절한다`() {
        service.importResult(
            command().copy(
                places =
                    listOf(
                        place(1).copy(imageUrls = listOf("", "  ")),
                        place(2),
                    ),
            ),
        )

        assertRejected("missing images")
    }

    @Test
    fun `도보 계산이 실패하면 모든 구간을 미확인으로 두고 코스를 생성한다`() {
        walking.failure = IllegalStateException("temporary direction failure")

        service.importResult(command().copy(places = listOf(place(1), place(2), place(3))))

        val course = courses.commands.single()
        assertEquals(listOf(null, null, null), course.places.map { it.walkingMinutes })
        assertEquals(CrawlImportStatus.IMPORTED, persistence.rows["C9xYz"]?.status)
    }

    @Test
    fun `코스 저장 실패는 전파하고 완료 이력을 남기지 않는다`() {
        val failure = IllegalStateException("course persistence unavailable")
        courses.failure = failure

        val thrown = assertThrows(IllegalStateException::class.java) { service.importResult(command()) }

        assertSame(failure, thrown)
        assertTrue(persistence.saved.none { it.status == CrawlImportStatus.IMPORTED })
    }

    private fun assertRejected(reason: String) {
        assertEquals(
            CrawlImport("C9xYz", "20261027-0400", CrawlImportStatus.REJECTED, reason = reason),
            persistence.rows["C9xYz"],
        )
        assertTrue(walking.requests.isEmpty())
        assertTrue(courses.commands.isEmpty())
    }

    private fun command() =
        CrawlResultCommand(
            runId = "20261027-0400",
            shortcode = "C9xYz",
            sourceUrl = "https://www.instagram.com/p/C9xYz/",
            account = "eateat.mag",
            isCourse = true,
            courseReason = "두 장소를 순서대로 방문하는 코스",
            title = "성수 데이트",
            description = "카페와 산책을 함께 즐기는 코스",
            tags = listOf("데이트"),
            places = listOf(place(1), place(2)),
        )

    private fun place(order: Int) =
        CrawledPlaceCommand(
            order = order,
            kakaoPlaceId = "k$order",
            name = "장소 $order",
            category = "CAFE",
            lat = 37.5,
            lng = 127.0,
            address = "서울 성동구",
            description = "장소 설명 $order",
            imageUrls = listOf("https://cdn.example.com/$order.jpg"),
        )

    private class FakePersistence : CrawlImportPersistencePort {
        val rows = mutableMapOf<String, CrawlImport>()
        val saved = mutableListOf<CrawlImport>()

        override fun findByShortcode(shortcode: String): CrawlImport? = rows[shortcode]

        override fun claim(
            shortcode: String,
            runId: String,
        ): CrawlImport =
            rows.getOrPut(shortcode) {
                CrawlImport(shortcode, runId, CrawlImportStatus.FAILED, reason = "in progress")
            }

        override fun save(result: CrawlImport) {
            saved += result
            rows[result.shortcode] = result
        }
    }

    private class FakePlaces : CrawlPlacePort {
        val requests = mutableListOf<List<CrawledPlaceCommand>>()

        override fun resolve(places: List<CrawledPlaceCommand>): List<ResolvedCrawlPlace> {
            requests += places
            return places.map { place ->
                ResolvedCrawlPlace(
                    kakaoPlaceId = place.kakaoPlaceId,
                    id = 100L + place.order,
                    coordinate = Coordinate(listOf(37.61, 37.62, 37.63)[place.order - 1], 127.1),
                )
            }
        }
    }

    private class FakeWalking : CrawlWalkingPort {
        val requests = mutableListOf<List<Coordinate>>()
        var segments = listOf(7)
        var failure: RuntimeException? = null

        override fun walkingSegments(points: List<Coordinate>): List<Int> {
            requests += points
            failure?.let { throw it }
            return segments
        }
    }

    private class FakeCourses : CrawlCoursePort {
        val commands = mutableListOf<CuratedCourseCommand>()
        var failure: RuntimeException? = null

        override fun create(command: CuratedCourseCommand): Long {
            failure?.let { throw it }
            commands += command
            return 91L
        }
    }
}
